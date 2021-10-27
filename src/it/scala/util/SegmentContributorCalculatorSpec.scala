package util

import org.scalatest.{Matchers, WordSpec}
import top.top1.therealtuesdayweld.util.{Contribution, Contributions, SegmentContributorCalculator}

import util.CreditRoles._

class SegmentContributorCalculatorSpec extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with SegmentContributorCalculator {
  val segmentPid = "p123"

  "SegmentTitleCalculator" should {

    "provide composer and track for classical music, ignoring all other roles" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Puccini", Composer),
        Contributions(segmentPid, "Orchestra", Performer),
        Contributions(segmentPid, "Jonas Kaufmann", FeaturedArtist),
        Contributions(segmentPid, "Jazzy Jeff", Dj),
        Contributions(segmentPid, "Herbert von Karajan", "CONDUCTOR"),
        Contributions(segmentPid, "The Squealing Pigs", "CHOIR")
      ), "Recondita Armonia")

      result shouldBe Contribution(
        "Puccini",
        Some("Recondita Armonia")
      )
    }

    "provide multiple composers for classical music" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Mozart", Composer),
        Contributions(segmentPid, "Beethoven", Composer),
        Contributions(segmentPid, "Grieg", Composer),
        Contributions(segmentPid, "Orchestra", Performer),
        Contributions(segmentPid, "Jonas Kaufmann", FeaturedArtist)
      ), "The Monster Mash")

      result shouldBe Contribution(
        "Mozart, Beethoven Grieg",
        Some("The Monster Mash")
      )
    }

    "provide performer and track" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Paul Simon", Performer)
      ), "Call Me Al")

      result shouldBe Contribution(
        "Paul Simon",
        Some("Call Me Al")
      )
    }

    "provide multiple performers" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Paul Simon", Performer),
        Contributions(segmentPid, "Art Garfunkel", Performer)
      ), "The Boxer")

      result shouldBe Contribution(
        "Paul Simon Art Garfunkel",
        Some("The Boxer")
      )
    }

    "provide multiple performers and featured artists" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Paul Young", Performer),
        Contributions(segmentPid, "Sting", Performer),
        Contributions(segmentPid, "Bono", Performer),
        Contributions(segmentPid, "Bananarama", Performer),
        Contributions(segmentPid, "Midge Ure", FeaturedArtist),
        Contributions(segmentPid, "Phil Collins", FeaturedArtist)
      ), "Band Aid")

      result shouldBe Contribution(
        "Paul Young, Sting, Bono Bananarama",
        Some("Band Aid Midge Ure Phil Collins")
      )
    }

    "provide vs artist when performer and vs artist" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Alien", Performer),
        Contributions(segmentPid, "Predator", VsArtist)
      ), "Title")

      result shouldBe Contribution(
        "Alien Predator",
        Some("Title")
      )
    }

    "provide vs artist when multiple performers and vs artists" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Raphael", Performer),
        Contributions(segmentPid, "Donatello", Performer),
        Contributions(segmentPid, "Leonardo", Performer),
        Contributions(segmentPid, "Freddie", VsArtist),
        Contributions(segmentPid, "Jason", VsArtist)
      ), "Title")

      result shouldBe Contribution(
        "Raphael, Donatello Leonardo Freddie Jason",
        Some("Title")
      )
    }

    "treat DJ as a primary performer" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Calvin Harris", Dj),
        Contributions(segmentPid, "Dua Lipa", Performer)
      ), "Title")

      result shouldBe Contribution(
        "Calvin Harris Dua Lipa",
        Some("Title")
      )
    }

    "provide vs artists and 'other' contributors" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Jason", VsArtist),
        Contributions(segmentPid, "Freddie", "REMIX-ARTIST")
      ), "Title")

      result shouldBe Contribution(
        "Freddie Jason",
        Some("Title")
      )
    }

    "provide vs artist when no primary or 'other' artist" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "Laurel", VsArtist),
        Contributions(segmentPid, "Hardy", VsArtist)
      ), "Title")

      result shouldBe Contribution(
        "Laurel Hardy",
        Some("Title")
      )
    }

    "provide track as primary and no secondary if no credit" in {
      val result = calculateContribution(Nil, "Title")

      result shouldBe Contribution(
        "Title",
        None
      )
    }

    "ignore other contributors when not whitelisted" in {
      val result = calculateContribution(Seq(
        Contributions(segmentPid, "The Beatles", Performer),
        Contributions(segmentPid, "Someone", "FOO")
      ), "Title")

      result shouldBe Contribution(
        "The Beatles",
        Some("Title")
      )
    }
  }
}

object CreditRoles {
  val Composer = "COMPOSER"
  val Performer = "PERFORMER"
  val Dj = "DJ"
  val FeaturedArtist = "FEATURED-ARTIST"
  val VsArtist = "VS-ARTIST"
}
