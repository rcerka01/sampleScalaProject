package top.top1.therealtuesdayweld.config

import com.typesafe.config.{Config => AkkaConfig, ConfigFactory => AkkaConfigFactory}

trait Config {
  protected lazy val config: AkkaConfig = AkkaConfigFactory.load()

  lazy val serviceName = config.getString("service.name")
  lazy val httpInterface = config.getString("service.http.interface")
  lazy val httpPort = config.getInt("service.http.port")
  lazy val linkingByIsrcStreamEnabled = config.getBoolean("service.linking-by-isrc-stream-enabled")
  lazy val linkingByArtistAndTitleStreamEnabled = config.getBoolean("service.linking-by-artist-and-title-stream-enabled")

  lazy val errorDocumentationUrl = config.getString("service.error.documentation-url")

  lazy val backfillByIsrc = config.getBoolean("service.backfill-by-isrc")
  lazy val backfillByArtistAndName = config.getBoolean("service.backfill-by-artist-and-name")

  lazy val clientId = config.getString("client.spotify.client-id")
  lazy val clientSecret = config.getString("client.spotify.client-secret")

  lazy val spotifyClientAccountsHost = config.getString("client.spotify.accounts-host")
  lazy val spotifyClientApiHost = config.getString("client.spotify.api-host")
  lazy val spotifyClientProtocol = config.getString("client.spotify.protocol")
  lazy val spotifyClientPort = config.getInt("client.spotify.port")

  lazy val appleClientHost = config.getString("client.apple.host")
  lazy val appleClientProtocol = config.getString("client.apple.protocol")
  lazy val appleClientPort = config.getInt("client.apple.port")
  lazy val appleClientBearerToken = config.getString("client.apple.bearer-token")

  lazy val tupacClientProtocol = config.getString("client.tupac.protocol")
  lazy val tupacClientHost = config.getString("client.tupac.host")
  lazy val tupacClientPort = config.getInt("client.tupac.port")

  lazy val throttleLinkingByIsrcStreamInSeconds: Int = config.getInt("service.throttle-linking-by-isrc-stream-in-seconds")
  lazy val throttleLinkingByArtistAndTitleStreamInSeconds: Int = config.getInt("service.throttle-linking-by-artist-and-title-stream-in-seconds")
  lazy val throttleStreamByOneInSeconds: Int = config.getInt("service.throttle-stream-by-one-in-milliseconds")

  lazy val getTracksFromDBbyIsrc = config.getString("kamon.number-of-get-tracks-from-database-by-isrc")
  lazy val getTracksFromDBbyArtistAndName = config.getString("kamon.number-of-get-tracks-from-database-by-artist-and-name")

  lazy val appleSearchByIsrc = config.getString("kamon.number-of-calls-to-apple-search-by-isrc")
  lazy val appleSearchByArtistAndTitle = config.getString("kamon.number-of-calls-to-apple-search-by-artist-and-title")
  lazy val appleDBUpdates = config.getString("kamon.number-of-apple-database-updates")
  lazy val appleTupacUpdates = config.getString("kamon.number-of-apple-tupac-updates")
  lazy val appleDBDeletes = config.getString("kamon.number-of-apple-database-deletes")
  lazy val appleTupacDeletes = config.getString("kamon.number-of-apple-tupac-deletes")

  lazy val spotifySearchByIsrc = config.getString("kamon.number-of-calls-to-spotify-search-by-isrc")
  lazy val spotifySearchByArtistAndTitle = config.getString("kamon.number-of-calls-to-spotify-search-by-artist-and-title")
  lazy val spotifyDBUpdates = config.getString("kamon.number-of-spotify-database-updates")
  lazy val spotifyTupacUpdates = config.getString("kamon.number-of-spotify-tupac-updates")
  lazy val spotifyDBDeletes = config.getString("kamon.number-of-spotify-database-deletes")
  lazy val spotifyTupacDeletes = config.getString("kamon.number-of-spotify-tupac-deletes")

  lazy val snsTupacActions: String = config.getString("aws.sns.tupac-actions-topic")
}
