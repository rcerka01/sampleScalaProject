package util

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.specs2.mutable.{BeforeAfter, Specification}
import top.top1.therealtuesdayweld.client.SegmentsDbClient
import top.top1.therealtuesdayweld.config.Config
import top.top1.therealtuesdayweld.domain.marshalling.JsonSerializers

import scala.util.Random
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Try

case class DatabaseTestSetupException(message: String) extends Exception(message)

abstract class AbstractSpec extends Specification
  with JsonSerializers {

  sequential

  implicit val timeout = RouteTestTimeout(5.seconds)

  trait AbstractScope extends BeforeAfter with Config {

    private val host: String = "127.0.0.1"
    private val port: Int = 8443

    private val wireMockServer = new WireMockServer(
      wireMockConfig()
        .port(port)
    )

    private val database: Database = Database.forConfig("database-it")

    private lazy val createMusicTable: Unit = executeCreate {
      sql"""
         set mode MySQL;
        CREATE TABLE programmes.music (
           music_id      VARCHAR(255),
           segment_id    VARCHAR(255),
           spotify_url   VARCHAR(255),
           apple_url     VARCHAR(255),
           is_fallback   BOOLEAN,
           is_editorial  BOOLEAN
         );
         CREATE UNIQUE INDEX ON programmes.music (music_id, segment_id);
      """
    }

    private lazy val createSegmentsTable: Unit = executeCreate {
      sql"""
        CREATE TABLE programmes.segment (
           pid           VARCHAR(255),
           isrc_id       VARCHAR(255),
           record_id     VARCHAR(255),
           type          VARCHAR(255),
           duration      INTEGER,
           title         VARCHAR(255),
           sequencer_int INTEGER
         )
        """
    }

    private lazy val createContributorTable: Unit = executeCreate {
      sql"""
        CREATE TABLE programmes.contributor (
           pid           VARCHAR(255),
           type          VARCHAR(255),
           name          VARCHAR(255),
           sequencer_int INTEGER
         )
        """
    }

    private lazy val createContributionTable: Unit = executeCreate {
      sql"""
        CREATE TABLE programmes.contribution (
           pid             VARCHAR(255),
           contributor_id  VARCHAR(255),
           segment_id      VARCHAR(255),
           position        INTEGER,
           credit_role_id  VARCHAR(255),
           sequencer_int   INTEGER
         )
        """
    }

    private lazy val createSegmentEventTable: Unit = executeCreate {
      sql"""
        CREATE TABLE programmes.segment_event (
           segment_id      VARCHAR(255),
           version_id      VARCHAR(255)
         )
        """
    }

    private lazy val createVersionsTable: Unit = executeCreate {
      sql"""
        CREATE TABLE programmes.versions (
           version_id      VARCHAR(255),
           pid              VARCHAR(255)
         )
        """
    }

    def dropAllTables(): Unit = {
      dropTable("programmes.music")
      dropTable("programmes.segment")
      dropTable("programmes.contributor")
      dropTable("programmes.contribution")
      dropTable("programmes.versions")
      dropTable("programmes.segment_event")
    }

    def before: Unit = {
      wireMockServer.start()

      WireMock.configureFor(host, port)

      createAllTables()
    }

    def after: Unit = {
      wireMockServer.stop()

      dropAllTables()

      database.close()
    }

    def readResourceFile(path: String): String = {
      Source.fromURL(getClass.getResource(path)).mkString
    }

    def createAllTables(): Unit = {
      createMusicTable
      createSegmentsTable
      createContributionTable
      createContributorTable
      createSegmentEventTable
      createVersionsTable
    }

    private def randomId(size: Int) = {
      Random.alphanumeric.take(size).mkString
    }

    def insertSegment(musicId: String,
                      isrc: Option[String],
                      title: String,
                      artist: String,
                      segmentId: String = randomId(5),
                      contributionId: String = randomId(5),
                      contributorId: String = randomId(5),
                      segmentVersionId: Option[String] = Some(randomId(5)),
                      versionId: Option[String] = Some(randomId(5)),
                      pid: String = randomId(5),
                      musicType: String = "music"
                     )
    : Unit = executeUpdate {
      sql"""
      INSERT INTO programmes.segment (
        pid,
        isrc_id,
        record_id,
        type,
        title
      ) VALUES (
        '#$segmentId',
         #${isrc.map(i => "'" + i + "'").getOrElse("NULL")},
        '#$musicId',
        '#$musicType',
        '#$title'
       );

      INSERT INTO programmes.contributor (
        pid,
        name
      ) VALUES (
        '#$contributorId',
        '#$title'
      );

      INSERT INTO programmes.contribution (
        pid,
        contributor_id,
        segment_id,
        credit_role_id
      ) VALUES (
        '#$contributionId',
        '#$contributorId',
        '#$segmentId',
        'PERFORMER'
      );

      INSERT INTO programmes.segment_event (
        segment_id,
        version_id
      ) VALUES (
        '#$segmentId',
        '#$segmentVersionId'
       );

       INSERT INTO programmes.versions (
         version_id,
         pid
       ) VALUES (
         '#$versionId',
         '#$segmentId'
        );
    """}

    def insertTrack(musicId: String,
                    isrc: Option[String],
                    spotify: String,
                    apple: String,
                    title: String,
                    artist: String,
                    isFallback: Boolean = false,
                    isEditorial: Boolean = false,
                    segmentId: String = randomId(5),
                    contributionId: String = randomId(5),
                    contributorId: String = randomId(5),
                    segmentVersionId: Option[String] = Some("kim"),
                    versionId: Option[String] = Some("kim")): Unit = {

      insertSegment(musicId, isrc, title, artist, segmentId, contributionId, contributorId, segmentVersionId, versionId)

      executeUpdate {
        sql"""
        INSERT INTO programmes.music (
          music_id,
          spotify_url,
          apple_url,
          is_fallback,
          is_editorial,
          segment_id
        ) VALUES (
          '#$musicId',
          '#$spotify',
          '#$apple',
          '#$isFallback',
          '#$isEditorial',
          '#$segmentId'
        );
      """
      }
    }

    private def dropTable(tableName: String): Unit = {
      executeDrop(sql"""DROP TABLE #$tableName""")   }

    private def executeCreate(sql: SQLActionBuilder): Unit = {
      Await.result(database.run(sql.as[Int]), Duration.Inf).headOption match {
        case Some(0) => ()
        case _ => throw DatabaseTestSetupException(s"SQL Create failure ($sql)")
      }
    }

    private def executeDrop(sql: SQLActionBuilder): Unit = Try {
      Await.result(database.run(sql.as[Int]), Duration.Inf)
    }

    private def executeUpdate(sql: SQLActionBuilder): Unit = {
      Await.result(database.run(sql.as[Int]), Duration.Inf).headOption match {
        case Some(1) => ()
        case _ => throw DatabaseTestSetupException(s"SQL Insert failure ($sql)")
      }
    }

    implicit val system: ActorSystem = ActorSystem("rms-tuesday-weld")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    implicit def executor: ExecutionContextExecutor = system.dispatcher

    val segmentsTestDbClient: SegmentsDbClient = new SegmentsDbClient(
      database = database
    )(system.dispatcher)
  }

}
