# PB Exporter

Exports selected boss personal bests stored by RuneLite.

## Features

- Copy selected personal bests to the clipboard as JSON.
- Upload personal bests to a configured server on logout.
- Three-hour cooldown per character after a successful upload.
- Export supported team sizes in a predefined order.
- Read existing personal bests without modifying them.

## Configuration

- Server URL: the API endpoint provided by your server administrator.
- API Token: your personal authentication token for that server.
- Copy raw data: copy the exported JSON while logged in.
  Server configuration is not required for clipboard export.

Uploading requires both a server URL and an API token.
Closing the client does not trigger an upload.

## Data sharing

Uploads send your RuneScape character name and selected personal bests
to the configured server. Personal bests include boss names, team sizes
where applicable, and completion times.

The receiving service can see the IP address used for the connection.
The configured server is not operated or verified by RuneLite.