package top.top1.therealtuesdayweld.util

import top.top1.therealtuesdayweld.client.FallbackMusicSegment
import top.top1.therealtuesdayweld.config.Config

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Contributions(segmentPid: String, contributorName: String, creditRoleId: String)

private object CreditRoles {
  val Composer = "COMPOSER"
  val Performer = "PERFORMER"
  val Dj = "DJ"
  val FeaturedArtist = "FEATURED-ARTIST"
  val VsArtist = "VS-ARTIST"
}

private object CreditAttribution extends Enumeration {
  val Primary, Featured, Vs, Other, Ignored = Value
}

case class Contribution(artist: String, title: Option[String])

trait SegmentContributorCalculator extends Config {

  private val otherCreditRoles = Map(
    "REMIX-ARTIST" -> "Remix Artist"
  )

  private def groupContributions(contributions: Seq[Contributions]) = {
    val groupingFunction = if (contributions.exists(_.creditRoleId == Composer)) {
      groupContributionsClassical
    } else {
      groupContributionsPopular
    }
    groupingFunction(contributions).withDefaultValue(Nil)
  }

  private val groupContributionsClassical = (contributions: Seq[Contributions]) => {
    contributions.groupBy {
      case Contributions(_, _, Composer) => CreditAttribution.Primary
      case _ => CreditAttribution.Ignored
    }
  }

  private val groupContributionsPopular = (contributions: Seq[Contributions]) => {
    val contributionsGrouped = contributions.groupBy {
      case contribution if Seq(Performer, Dj).contains(contribution.creditRoleId) => CreditAttribution.Primary
      case Contributions(_, _, FeaturedArtist) => CreditAttribution.Featured
      case Contributions(_, _, VsArtist) => CreditAttribution.Vs
      case Contributions(_, _, other) if otherCreditRoles.contains(other) => CreditAttribution.Other
      case _ => CreditAttribution.Ignored
    }.withDefaultValue(Nil)

    (contributionsGrouped(CreditAttribution.Primary),
      contributionsGrouped(CreditAttribution.Other),
      contributionsGrouped(CreditAttribution.Vs)) match {

      case (Nil, otherHead :: otherTail, _) =>
        contributionsGrouped ++ Map(
          CreditAttribution.Primary -> Seq(otherHead),
          CreditAttribution.Other -> otherTail
        )
      case (Nil, Nil, vsHead :: vsTail) =>
        contributionsGrouped ++ Map(
          CreditAttribution.Primary -> Seq(vsHead),
          CreditAttribution.Vs -> vsTail
        )
      case _ => contributionsGrouped
    }
  }

  private def constructPrimary(primaryArtists: Seq[String], vsArtists: Seq[String]) = Seq(
    joinNamesOptionally(primaryArtists),
    joinNamesOptionally(vsArtists)
  ).flatten.mkString(" ")

  private def constructSecondary(recordTitle: String, featuredArtists: Seq[String]) = if (featuredArtists.nonEmpty) {
    s"$recordTitle ${joinNames(featuredArtists)}"
  } else {
    recordTitle
  }

  private def joinNames(strings: Seq[String]): String = {
    strings match {
      case head :: Nil => head
      case init :+ last =>
        s"${init.mkString(", ")} $last"
      case _ => ""
    }
  }

  private def joinNamesOptionally(strings: Seq[String]): Option[String] = {
    strings match {
      case head :: Nil => Some(head)
      case init :+ last =>
        Some(s"${init.mkString(", ")} $last")
      case _ => None
    }
  }

  def calculateContribution(segmentContributions: Seq[Contributions], recordTitle: String): Contribution = {
    val groupedContributions = groupContributions(segmentContributions)

    val primaryArtists = groupedContributions(CreditAttribution.Primary).map(_.contributorName)
    val featuredArtists = groupedContributions(CreditAttribution.Featured).map(_.contributorName)
    val vsArtists = groupedContributions(CreditAttribution.Vs).map(_.contributorName)

    val (artist, title) = if (primaryArtists.nonEmpty) {
      val secondary = constructSecondary(recordTitle, featuredArtists)
      val primary = constructPrimary(primaryArtists, vsArtists)
      (primary, Some(secondary))
    } else {
      (recordTitle, None)
    }

    Contribution(
      title = title,
      artist = artist)
  }

  def updateContributor(futureFallbackItems: Future[Seq[FallbackMusicSegment]]): Future[Seq[FallbackMusicSegment]] = {
    futureFallbackItems map { fallbackItems =>

      val fallbackItemsWithIds = fallbackItems.filter ( item => item.musicId.isDefined && item.segmentId.isDefined )

      val mapItems: Map[(Option[String], Option[String]), Seq[FallbackMusicSegment]] = fallbackItemsWithIds.groupBy(item => (item.musicId, item.segmentId))

      (for ((key, value) <- mapItems) yield {

        val contributions: List[Contributions] = (value flatMap { item =>
          item.segmentId flatMap { segmentId =>
            item.artist.flatMap { artist =>
              item.creditRole.map { role =>
                Contributions(segmentId, artist, role)
              }
            }
          }
        }).toList.distinct

        val title = value.headOption flatMap ( _.title )

        val calculatedContributionOption = title map { title => calculateContribution(contributions, title) }

        calculatedContributionOption flatMap { contribution =>
          value.headOption.map { item =>
            item.copy(artist = Some(contribution.artist), title = contribution.title)
          }
        }

      }).toSeq.flatten
    }
  }

}
