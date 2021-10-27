package top.top1.therealtuesdayweld.domain.tupac

object TupacResponse {

  case class AudioResource(
                            audioType: Option[String],
                            resourceType: Option[String]
                          )

  case class MusicAudio(
                         resourceType: Option[String],
                         identifier: Option[String],
                         audioType: Option[String],
                         raw: Option[Boolean],
                         audioResource: Option[AudioResource],
                         safe: Option[Boolean]
                       )

  case class MusicArtist(
                          gid: Option[String],
                          name: Option[String],
                          sortName: Option[String],
                          imagePid: Option[String],
                          imageFormat: Option[String],
                          imageUrl: Option[String],
                          squareImagePid:  Option[String],
                          squareImageFormat: Option[String],
                          squareImageUrl:  Option[String],
                          role: Option[String]
                        )

  case class MusicTrackEntryItem(
                                  id: Option[String],
                                  `type`: Option[String],
                                  title: Option[String],
                                  recordImagePid: Option[String],
                                  recordImageFormat: Option[String],
                                  recordImageUrl: Option[String],
                                  radioEdit: Option[Boolean],
                                  album: Option[String],
                                  classical: Option[Boolean],
                                  baseTitle: Option[String],
                                  pipsName: Option[String],
                                  artistGid: Option[String],
                                  artistName: Option[String],
                                  artists: Option[List[MusicArtist]],
                                  recordAudio: Option[List[MusicAudio]],
                                  preferredRecordAudio: Option[MusicAudio],
                                  preferredMultiplayRecordAudio: Option[MusicAudio],
                                  `external-links`: Option[List[MusicExternalPartner]]
                                )

  case class MusicTrackEntrySummaryItem(
                                         id: Option[String],
                                         title: Option[String],
                                         artistName: Option[String],
                                         artists: Option[List[MusicArtist]],
                                         `external-links`: Option[List[MusicExternalPartner]]
                                       )
  object MusicTrackEntrySummaryItem{
    def apply(musicTrackEntryItem: MusicTrackEntryItem):MusicTrackEntrySummaryItem = {
      MusicTrackEntrySummaryItem(
        musicTrackEntryItem.id,
        musicTrackEntryItem.title,
        musicTrackEntryItem.artistName,
        musicTrackEntryItem.artists,
        musicTrackEntryItem.`external-links`
      )
    }
  }


  case class MusicTrackEntry(
                              position: Option[Int],
                              item: Option[MusicTrackEntryItem]
                            )

  case class MusicCurrentTracks(
                                 date: Option[String],
                                 hasSections: Option[Boolean],
                                 entries: Option[List[MusicTrackEntry]]
                               )

  case class MusicExternalPartner(
                                   `type`: Option[String],
                                   value: Option[String]
                                 )

  case class MusicData(
                        id: Option[String],
                        title: Option[String],
                        description: Option[String],
                        serviceId: Option[String],
                        kind: Option[String],
                        categories: Option[List[String]],
                        externalPartners: Option[List[MusicExternalPartner]],
                        updatedAt: Option[String],
                        oneOff: Option[Boolean],
                        imagePid: Option[String],
                        imageUrl: Option[String],
                        wideImagePid: Option[String],
                        wideImageUrl: Option[String],
                        visibility: Option[String],
                        commentsEnabled: Option[Boolean],
                        playlistPlaybackType: Option[String],
                        `current-tracks`: Option[MusicCurrentTracks],
                        genre: Option[String],
                        mood: Option[String],
                        version: Option[MusicPlaylistVersion]
                      )

  case class MusicRecommendedFrom(
                                   id: Option[String],
                                   title: Option[String],
                                   artistsDisplay: Option[String],
                                   primaryArtistGid: Option[String]
                                 )

  case class MusicPlaylist(
                            id: Option[String],
                            title: Option[String],
                            description: Option[String],
                            serviceId: Option[String],
                            imagePid: Option[String],
                            updatedAt: Option[String],
                            recommendedFrom: Option[MusicRecommendedFrom]
                          )

  case class MusicPlaylistVersion(
                                   vpid: Option[String],
                                   duration: Option[String],
                                   startsAt: Option[String],
                                   expiresAt: Option[String],
                                   guidanceText: Option[String],
                                   warnings: List[String]
                                 )

  case class MusicParentProgramme(
                                   pid: Option[String],
                                   title: Option[String],
                                   entityType: Option[String]
                                 )

  case class MusicVersion(
                           vpid: Option[String],
                           duration: Option[String]
                         )

  case class MusicClip(
                        pid: Option[String],
                        `type`: Option[String],
                        title: Option[String],
                        image: Option[String],
                        duration: Option[String],
                        synopsis: Option[String],
                        imageUrl: Option[String],
                        mediaType: Option[String],
                        entityType: Option[String],
                        startDate: Option[String],
                        endDate: Option[String],
                        masterbrandMid: Option[String],
                        parentProgramme: Option[MusicParentProgramme],
                        contributors: Option[List[String]],
                        version: Option[MusicVersion],
                        recommendedFrom: Option[MusicRecommendedFrom]
                      )


  case class MusicRecommendationsSet(
                                      playlist: Option[MusicPlaylist],
                                      clip: Option[MusicClip]
                                    )

  case class MusicRecommendationsData(
                                       trackCount: Option[Int],
                                       moreTracksOffset: Option[Int],
                                       sets: Option[List[MusicRecommendationsSet]]
                                     )

  case class MusicGenresData(pipsId: Option[String], urlKey: Option[String], localeKey: Option[String], englishTitle: Option[String])

  case class MusicServiceData(id: Option[String], name: Option[String], urlKey: Option[String])

  sealed abstract class MusicResponse()

  case class MusicGenresResponse(data: Option[List[MusicGenresData]]) extends MusicResponse

  case class MusicArtistResponse(data: Option[MusicArtist]) extends MusicResponse

  case class MusicTrackResponse(data: Option[MusicTrackEntryItem]) extends MusicResponse

  case class MusicTracksResponse(data: List[MusicTrackEntryItem]) extends MusicResponse

  case class MusicClipResponse(data: Option[List[MusicClip]]) extends MusicResponse

  case class MusicPlaylistDetailsResponse(data: Option[MusicData]) extends MusicResponse

  case class MusicNowPlayingServicesResponse(data: Option[List[MusicServiceData]]) extends MusicResponse

  case class MusicServicesPopularityServicesResponse(data: Option[List[MusicServiceData]]) extends MusicResponse

  case class MusicRecommendationsResponse(data: MusicRecommendationsData) extends MusicResponse

}
