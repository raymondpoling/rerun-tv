module LocationTests

open System
open Xunit
open Library.LocationComparison
open Newtonsoft.Json.Linq
open TheseTypes.TestResults


[<Fact>]
let ``All locations are equal``() =
    let input = new JArray(new JValue("http://archive/video/Show1/Season 1/Episode1-1.mkv"),
                            new JValue("file://CrystalBall/home/ruguer/Videos/Show1/Season 1/Episode1-1.mkv"),
                            new JValue("ftp://archive/volume1/video/Show1/Season 1/Episode1-1.mkv"))
    let roots = ["http://archive/video";
        "file://CrystalBall/home/ruguer/Videos";
        "ftp://archive/volume1/video"]
    let actual = compareLocations roots input
    Assert.Equal(Success, actual)

[<Fact>]
let ``A location fails``() =
    let input = new JArray(new JValue("http://archive/video/Show1/Season 1/Episode1-2.mkv"),
                            new JValue("file://CrystalBall/home/ruguer/Videos/Show1/Season 1/Episode1-1.mkv"),
                            new JValue("ftp://archive/volume1/video/Show1/Season 1/Episode1-1.mkv"))
    let roots = ["http://archive/video";
        "file://CrystalBall/home/ruguer/Videos";
        "ftp://archive/volume1/video"]
    let actual = compareLocations roots input
    Assert.Equal(Failures(Message "Files do not match.", 
                                    Errors ["http://archive/video/Show1/Season 1/Episode1-2.mkv";
                                    "file://CrystalBall/home/ruguer/Videos/Show1/Season 1/Episode1-1.mkv";
                                    "ftp://archive/volume1/video/Show1/Season 1/Episode1-1.mkv"])
                ,actual)

[<Fact>]
let ``A location is missing``() =
    let input = new JArray(new JValue("http://archive/video/Show1/Season 1/Episode1-1.mkv"),
                            new JValue("ftp://archive/volume1/video/Show1/Season 1/Episode1-1.mkv"))
    let roots = ["http://archive/video";
        "file://CrystalBall/home/ruguer/Videos";
        "ftp://archive/volume1/video"]
    let actual = compareLocations roots input
    Assert.Equal(Failures(Message "Mismatched roots.",
                                     Errors ["http://archive/video/Show1/Season 1/Episode1-1.mkv";
                                    "ftp://archive/volume1/video/Show1/Season 1/Episode1-1.mkv"])
                ,actual)

[<Fact>]
let ``A location is duplicated``() =
    let input = new JArray(new JValue("http://archive/video/Show1/Season 1/Episode1-1.mkv"),
                            new JValue("ftp://archive/volume1/video/Show1/Season 1/Episode1-1.mkv"),
                            new JValue("http://archive/video/Show1/Season 1/Episode1-1.mkv"))
    let roots = ["http://archive/video";
        "file://CrystalBall/home/ruguer/Videos";
        "ftp://archive/volume1/video"]
    let actual = compareLocations roots input
    Assert.Equal(Failures(Message "Mismatched roots.", 
                                    Errors ["http://archive/video/Show1/Season 1/Episode1-1.mkv";
                                    "ftp://archive/volume1/video/Show1/Season 1/Episode1-1.mkv";
                                    "http://archive/video/Show1/Season 1/Episode1-1.mkv"])
                ,actual)