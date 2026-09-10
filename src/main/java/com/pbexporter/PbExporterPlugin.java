package com.pbexporter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
	name = "PB Exporter",
	description = "Exports RuneLite Personal Bests to the clipboard or a remote server",
	tags = {"pb", "personal best", "export"}
)
public class PbExporterPlugin extends Plugin
{
	private static final MediaType JSON =
		MediaType.get("application/json; charset=utf-8");

	private static final long UPLOAD_COOLDOWN_MS =
		TimeUnit.HOURS.toMillis(3);

	private static final String LAST_UPLOAD_PREFIX = "lastUpload.";

	private static final Pattern SCALE_PATTERN = Pattern.compile(
		"^(?<boss>.+?)\\s+(?<scale>solo|\\d+(?:\\+|-\\d+)? players?)$",
		Pattern.CASE_INSENSITIVE
	);

	private static final Map<String, List<String>> EXPORT_LIST =
		new LinkedHashMap<>();

	static
	{
		// =========================
		// Fight Caves / Inferno
		// =========================

		EXPORT_LIST.put(
			"tztok-jad",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"tzkal-zuk",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"tzhaar-ket-rak's first challenge",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"tzhaar-ket-rak's second challenge",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"tzhaar-ket-rak's third challenge",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"tzhaar-ket-rak's fourth challenge",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"tzhaar-ket-rak's fifth challenge",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"tzhaar-ket-rak's sixth challenge",
			Collections.emptyList()
		);


		// =========================
		// Other bosses
		// =========================

		EXPORT_LIST.put(
			"sol heredit",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"phosani's nightmare",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"gauntlet",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"corrupted gauntlet",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"yama",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"amoxliatl",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"mad angel",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"royal titans",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"vorkath",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"zulrah",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"phantom muspah",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"maggot king",
			Collections.emptyList()
		);


		// =========================
		// DT2 / Demonic Brutus
		// =========================

		EXPORT_LIST.put(
			"duke sucellus",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"duke sucellus (awakened)",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"leviathan",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"leviathan (awakened)",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"vardorvis",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"vardorvis (awakened)",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"whisperer",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"whisperer (awakened)",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"demonic brutus",
			Collections.emptyList()
		);


		// =========================
		// Slayer / misc
		// =========================

		EXPORT_LIST.put(
			"shellbane gryphon",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"grotesque guardians",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"araxxor",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"alchemical hydra",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"hespori",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"mimic",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"galvek",
			Collections.emptyList()
		);

		EXPORT_LIST.put(
			"fragment of seren",
			Collections.emptyList()
		);


		// =========================
		// Theatre of Blood
		// =========================

		EXPORT_LIST.put(
			"theatre of blood",
			Arrays.asList(
				"2 players",
				"3 players",
				"4 players",
				"5 players"
			)
		);

		EXPORT_LIST.put(
			"theatre of blood hard mode",
			Arrays.asList(
				"3 players",
				"4 players",
				"5 players"
			)
		);


		// =========================
		// Chambers of Xeric
		// =========================

		EXPORT_LIST.put(
			"chambers of xeric",
			Arrays.asList(
				"solo",
				"3 players",
				"5 players"
			)
		);

		EXPORT_LIST.put(
			"chambers of xeric challenge mode",
			Arrays.asList(
				"solo",
				"3 players",
				"5 players"
			)
		);


		// =========================
		// Tombs of Amascut
		// =========================

		EXPORT_LIST.put(
			"tombs of amascut",
			Arrays.asList(
				"solo",
				"2 players",
				"3 players",
				"4 players",
				"8 players"
			)
		);

		EXPORT_LIST.put(
			"tombs of amascut expert mode",
			Arrays.asList(
				"solo",
				"2 players",
				"3 players",
				"4 players",
				"8 players"
			)
		);


		// =========================
		// Nightmare
		// =========================

		EXPORT_LIST.put(
			"nightmare",
			Arrays.asList(
				"solo",
				"5 players"
			)
		);
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PbExporterConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	private volatile Session activeSession;

	@Provides
	PbExporterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PbExporterConfig.class);
	}

	@Override
	protected void startUp()
	{
		Gson exportGson = gson.newBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.serializeNulls()
			.create();

		OkHttpClient uploadClient = okHttpClient.newBuilder()
			.followRedirects(false)
			.followSslRedirects(false)
			.callTimeout(30, TimeUnit.SECONDS)
			.build();

		Session session = new Session(exportGson, uploadClient);
		activeSession = session;

		clientThread.invokeLater(() ->
		{
			if (activeSession == session)
			{
				initializeSession(session);
			}
		});

		log.info("PB Exporter started");
	}

	@Override
	protected void shutDown()
	{
		Session session = activeSession;
		activeSession = null;

		if (session != null)
		{
			for (Call call : session.uploads.values())
			{
				call.cancel();
			}
		}

		log.info("PB Exporter stopped");
	}

	private void initializeSession(Session session)
	{
		assert client.isClientThread();

		if (session.initialized)
		{
			return;
		}

		session.initialized = true;
		GameState gameState = client.getGameState();
		session.wasLoggedIn = gameState == GameState.LOGGED_IN;
		session.worldHopInProgress = gameState == GameState.HOPPING;

		if (session.wasLoggedIn)
		{
			cachePlayer(session);
		}
	}

	private Session getClientSession()
	{
		assert client.isClientThread();

		Session session = activeSession;
		if (session != null)
		{
			initializeSession(session);
		}
		return session;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		Session session = getClientSession();
		if (session == null)
		{
			return;
		}

		GameState newState = event.getGameState();
		if (newState == GameState.LOGGED_IN)
		{
			session.wasLoggedIn = true;
			session.worldHopInProgress = false;

			if (!cachePlayer(session))
			{
				session.currentNickname = null;
				session.currentRsProfile = null;
			}
			return;
		}

		if (newState == GameState.HOPPING)
		{
			session.worldHopInProgress = true;
			return;
		}

		if (newState == GameState.LOGIN_SCREEN)
		{
			boolean shouldUpload =
				session.wasLoggedIn && !session.worldHopInProgress;
			String nickname = session.currentNickname;
			String rsProfile = session.currentRsProfile;

			session.wasLoggedIn = false;
			session.worldHopInProgress = false;
			session.currentNickname = null;
			session.currentRsProfile = null;

			if (shouldUpload)
			{
				log.debug("PB Exporter: logout detected for {}", nickname);
				uploadPersonalBestsIfAllowed(session, nickname, rsProfile);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Session session = getClientSession();
		if (session != null && client.getGameState() == GameState.LOGGED_IN)
		{
			session.wasLoggedIn = true;
			session.worldHopInProgress = false;
			cachePlayer(session);
		}
	}

	private boolean cachePlayer(Session session)
	{
		assert client.isClientThread();

		Player player = client.getLocalPlayer();
		String rsProfile = configManager.getRSProfileKey();
		if (player == null || rsProfile == null || rsProfile.trim().isEmpty())
		{
			return false;
		}

		String nickname = player.getName();
		if (nickname == null)
		{
			return false;
		}

		nickname = nickname.replace('\u00a0', ' ').trim();
		if (nickname.isEmpty())
		{
			return false;
		}

		if (!nickname.equals(session.currentNickname) ||
			!rsProfile.equals(session.currentRsProfile))
		{
			log.debug("PB Exporter: cached player {}", nickname);
		}

		session.currentNickname = nickname;
		session.currentRsProfile = rsProfile;
		return true;
	}


	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		Session session = activeSession;
		if (session == null ||
			!PbExporterConfig.GROUP.equals(event.getGroup()) ||
			!"copyRawData".equals(event.getKey()) ||
			!Boolean.parseBoolean(event.getNewValue()))
		{
			return;
		}

		try
		{
			clientThread.invokeLater(() ->
			{
				if (activeSession != session)
				{
					return;
				}

				initializeSession(session);
				try
				{
					copyPersonalBestsToClipboard(session);
				}
				catch (RuntimeException e)
				{
					log.warn("PB Exporter: could not prepare clipboard export", e);
				}
			});
		}
		finally
		{
			configManager.setConfiguration(
				PbExporterConfig.GROUP,
				"copyRawData",
				false
			);
		}
	}

	private void copyPersonalBestsToClipboard(Session session)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			log.warn("PB Exporter: cannot copy PBs while logged out");
			return;
		}

		if (!cachePlayer(session))
		{
			log.warn("PB Exporter: player or RS profile is not available");
			return;
		}

		JsonObject payload = buildPayload(session.currentRsProfile);
		String json = session.exportGson.toJson(payload);
		String nickname = session.currentNickname;
		int pbCount = payload.getAsJsonArray("personal_bests").size();

		SwingUtilities.invokeLater(() ->
		{
			if (activeSession != session)
			{
				return;
			}

			try
			{
				Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new StringSelection(json), null);
				log.info(
					"PB Exporter: copied {} personal bests for {} to clipboard",
					pbCount,
					nickname
				);
			}
			catch (IllegalStateException | HeadlessException | SecurityException e)
			{
				log.warn("PB Exporter: clipboard is unavailable; try copying again", e);
			}
		});
	}

	private void uploadPersonalBestsIfAllowed(
		Session session,
		String nickname,
		String rsProfile)
	{
		assert client.isClientThread();

		if (activeSession != session)
		{
			return;
		}

		if (nickname == null || nickname.trim().isEmpty() ||
			rsProfile == null || rsProfile.trim().isEmpty())
		{
			log.warn("PB Exporter: cached player or RS profile is not available");
			return;
		}

		String serverUrl = config.serverUrl().trim();
		String token = config.apiToken().trim();
		if (serverUrl.isEmpty() || token.isEmpty())
		{
			log.debug("PB Exporter: automatic upload is not configured");
			return;
		}

		HttpUrl url = HttpUrl.parse(serverUrl);

		if (url == null)
		{
			log.warn("PB Exporter: Server URL must be a valid URL");
			return;
		}

		boolean localhost =
			"127.0.0.1".equals(url.host()) ||
			"localhost".equalsIgnoreCase(url.host());

		if (!url.isHttps() && !localhost)
		{
			log.warn("PB Exporter: Server URL must use HTTPS");
			return;
		}

		if (!url.username().isEmpty() || !url.password().isEmpty())
		{
			log.warn("PB Exporter: Server URL must not contain a username or password");
			return;
		}

		String uploadKey = normalizeNickname(nickname);
		if (session.uploads.containsKey(uploadKey))
		{
			log.debug("PB Exporter: upload already in progress for {}", nickname);
			return;
		}

		long now = System.currentTimeMillis();
		long lastUpload = getLastUploadTime(nickname);
		long elapsed = now - lastUpload;
		if (lastUpload > 0 && elapsed < UPLOAD_COOLDOWN_MS)
		{
			long remaining = UPLOAD_COOLDOWN_MS - elapsed;
			log.debug(
				"PB Exporter: upload skipped for {}. Cooldown remaining: {} minutes",
				nickname,
				TimeUnit.MILLISECONDS.toMinutes(remaining)
			);
			return;
		}

		JsonObject payload = buildPayload(rsProfile);
		if (payload.getAsJsonArray("personal_bests").size() == 0)
		{
			log.debug("PB Exporter: no selected personal bests to upload for {}", nickname);
			return;
		}

		sendToServer(session, url, token, nickname, uploadKey, payload);
	}

	private void sendToServer(
		Session session,
		HttpUrl serverUrl,
		String token,
		String nickname,
		String uploadKey,
		JsonObject payload)
	{
		final Call requestCall;
		try
		{
			String json = session.exportGson.toJson(payload);
			RequestBody body = RequestBody.create(JSON, json);
			Request request = new Request.Builder()
				.url(serverUrl)
				.header("Authorization", "Bearer " + token)
				.header("X-RuneScape-Name", nickname)
				.post(body)
				.build();
			requestCall = session.uploadClient.newCall(request);
		}
		catch (IllegalArgumentException e)
		{
			log.warn("PB Exporter: invalid URL, API Token or nickname; request was not sent");
			return;
		}

		if (session.uploads.putIfAbsent(uploadKey, requestCall) != null)
		{
			return;
		}

		if (activeSession != session)
		{
			session.uploads.remove(uploadKey, requestCall);
			requestCall.cancel();
			return;
		}

		log.info(
			"PB Exporter: sending {} personal bests for {}",
			payload.getAsJsonArray("personal_bests").size(),
			nickname
		);

		try
		{
			requestCall.enqueue(new Callback()
			{
				@Override
				public void onFailure(Call call, IOException e)
				{
					clientThread.invokeLater(() ->
					{
						try
						{
							if (activeSession == session && !call.isCanceled())
							{
								log.warn(
									"PB Exporter: upload failed for {} ({})",
									nickname,
									e.getClass().getSimpleName()
								);
							}
						}
						finally
						{
							session.uploads.remove(uploadKey, call);
						}
					});
				}

				@Override
				public void onResponse(Call call, Response response)
				{
					final int statusCode;
					try (Response r = response)
					{
						statusCode = r.code();
					}
					long completedAt = System.currentTimeMillis();

					clientThread.invokeLater(() ->
					{
						try
						{
							if (activeSession != session)
							{
								return;
							}

							if (statusCode >= 200 && statusCode < 300)
							{
								setLastUploadTime(nickname, completedAt);
								log.info(
									"PB Exporter: sent successfully for {}. HTTP {}",
									nickname,
									statusCode
								);
							}
							else
							{
								log.warn(
									"PB Exporter: server rejected upload for {}. HTTP {}",
									nickname,
									statusCode
								);
							}
						}
						finally
						{
							session.uploads.remove(uploadKey, call);
						}
					});
				}
			});
		}
		catch (RuntimeException e)
		{
			session.uploads.remove(uploadKey, requestCall);
			requestCall.cancel();
			log.warn(
				"PB Exporter: could not schedule upload for {} ({})",
				nickname,
				e.getClass().getSimpleName()
			);
		}
	}

	/*
	 * COOLDOWN
	 */
	private long getLastUploadTime(String nickname)
	{
		String value = configManager.getConfiguration(
			PbExporterConfig.GROUP,
			getLastUploadKey(nickname)
		);
		if (value == null)
		{
			return 0L;
		}

		try
		{
			return Long.parseLong(value);
		}
		catch (NumberFormatException e)
		{
			return 0L;
		}
	}

	private void setLastUploadTime(String nickname, long timestamp)
	{
		configManager.setConfiguration(
			PbExporterConfig.GROUP,
			getLastUploadKey(nickname),
			timestamp
		);
	}

	private String getLastUploadKey(String nickname)
	{
		return LAST_UPLOAD_PREFIX + normalizeNickname(nickname);
	}

	private String normalizeNickname(String nickname)
	{
		return nickname
			.replace('\u00a0', ' ')
			.trim()
			.toLowerCase(Locale.ROOT)
			.replace('-', ' ')
			.replace('_', ' ')
			.replaceAll("\\s+", "_");
	}

	private static final class Session
	{
		private final Gson exportGson;
		private final OkHttpClient uploadClient;

		private final Map<String, Call> uploads = new ConcurrentHashMap<>();

		private boolean initialized;
		private boolean wasLoggedIn;
		private boolean worldHopInProgress;
		private String currentNickname;
		private String currentRsProfile;

		private Session(Gson exportGson, OkHttpClient uploadClient)
		{
			this.exportGson = exportGson;
			this.uploadClient = uploadClient;
		}
	}

	private JsonObject buildPayload(String rsProfile)
	{
		List<String> pbKeys =
			configManager.getRSProfileConfigurationKeys(
				"personalbest",
				rsProfile,
				""
			);

		Map<String, Double> foundPbs =
			new HashMap<>();

		for (String key : pbKeys)
		{
			Double time =
				configManager.getConfiguration(
					"personalbest",
					rsProfile,
					key,
					double.class
				);

			if (time == null || !Double.isFinite(time) || time < 0)
			{
				continue;
			}

			PbName parsed =
				parsePbName(key);

			String boss =
				parsed.boss
					.trim()
					.toLowerCase(Locale.ROOT);

			String scale =
				parsed.scale == null
					? ""
					: parsed.scale
						.trim()
						.toLowerCase(Locale.ROOT);

			foundPbs.put(
				boss + "\n" + scale,
				time
			);
		}


		JsonArray personalBests =
			new JsonArray();

		for (Map.Entry<String, List<String>> entry :
			EXPORT_LIST.entrySet())
		{
			String boss =
				entry.getKey();

			List<String> scales =
				entry.getValue();


			if (scales.isEmpty())
			{
				Double time =
					foundPbs.get(
						boss + "\n"
					);

				if (time == null)
				{
					continue;
				}

				JsonObject pb =
					new JsonObject();

				pb.addProperty(
					"boss",
					boss
				);

				pb.add(
					"scale",
					com.google.gson.JsonNull.INSTANCE
				);

				pb.addProperty(
					"time",
					time
				);

				personalBests.add(pb);

				continue;
			}

			for (String scale : scales)
			{
				Double time =
					foundPbs.get(
						boss + "\n" + scale
					);

				if (time == null)
				{
					continue;
				}

				JsonObject pb =
					new JsonObject();

				pb.addProperty(
					"boss",
					boss
				);

				pb.addProperty(
					"scale",
					scale
				);

				pb.addProperty(
					"time",
					time
				);

				personalBests.add(pb);
			}
		}


		JsonObject root =
			new JsonObject();

		root.add(
			"personal_bests",
			personalBests
		);

		return root;
	}


	private PbName parsePbName(String key)
	{
		Matcher matcher =
			SCALE_PATTERN.matcher(key);

		if (matcher.matches())
		{
			return new PbName(
				matcher.group("boss"),
				matcher.group("scale")
			);
		}

		return new PbName(
			key,
			null
		);
	}


	private static class PbName
	{
		private final String boss;
		private final String scale;

		private PbName(
			String boss,
			String scale)
		{
			this.boss = boss;
			this.scale = scale;
		}
	}
}
