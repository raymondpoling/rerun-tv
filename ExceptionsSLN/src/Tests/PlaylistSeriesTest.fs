module PlaylistSeriesTest

open System
open Xunit
open Library.TestPlaylistSeries
open TheseTypes.TestResults
open Newtonsoft.Json.Linq

[<Fact>]
let ``SeriesPlaylist matches Series?``() =
    let series = 
        new JObject(
            new JProperty("status",new JValue("ok")),
            new JProperty("catalog-ids",new JArray(new JValue("TEST0101001"),new JValue("TEST0101002"),new JValue("TEST0101003"))))
    let playlist = 
        new JObject(
            new JProperty("status",new JValue("ok")),
            new JProperty("catalog-ids",new JArray(new JValue("TEST0101001"),new JValue("TEST0101002"),new JValue("TEST0101003"))))
    let actual = playlistSeriesTest series playlist
    Assert.Equal(Success,actual)

[<Fact>]
let ``SeriesPlaylist left failure?``() =
    let series = 
        new JObject(
            new JProperty("status",new JValue("ok")),
            new JProperty("catalog-ids",new JArray(new JValue("TEST0101001"),
                                                    new JValue("TEST0101002"),
                                                    new JValue("TEST0101003"),
                                                    new JValue("TEST0101004"))))
    let playlist = 
        new JObject(
            new JProperty("status",new JValue("ok")),
            new JProperty("catalog-ids",new JArray(new JValue("TEST0101001"),
                                                    new JValue("TEST0101002"),
                                                    new JValue("TEST0101003"))))
    let actual = playlistSeriesTest series playlist
    let expected = Failures (Message "Series playlist and series do not match", 
                            Errors ["left: TEST0101004";
                                    "right: ";
                                    "center: TEST0101001, TEST0101002, TEST0101003"])
    Assert.Equal(expected,actual)

[<Fact>]
let ``SeriesPlaylist right failure?``() =
    let series = 
        new JObject(
            new JProperty("status",new JValue("ok")),
            new JProperty("catalog-ids",new JArray(new JValue("TEST0101001"),
                                                    new JValue("TEST0101002"),
                                                    new JValue("TEST0101003"))))
    let playlist = 
        new JObject(
            new JProperty("status",new JValue("ok")),
            new JProperty("catalog-ids",new JArray(new JValue("TEST0101001"),
                                                    new JValue("TEST0101002"),
                                                    new JValue("TEST0101003"),
                                                    new JValue("TEST0101004"))))
    let actual = playlistSeriesTest series playlist
    let expected = Failures (Message "Series playlist and series do not match",
                            Errors ["left: ";
                                    "right: TEST0101004";
                                    "center: TEST0101001, TEST0101002, TEST0101003"])
    Assert.Equal(expected,actual)
