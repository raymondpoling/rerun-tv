module CorrectLocationsTests

open System
open Xunit
open CorrectLibrary.LocationCorrection

[<Fact>]
let ``One location of three wrong``() =
    let input = [("http://archive/video","/Show1/Season 1/Episode1-1.");
                    ("file://CrystalBall/home/ruguer/Videos","/Show1/Season 1/Episode1-1.mkv");
                    ("ftp://archive/volume1/video","/Show1/Season 1/Episode1-1.mkv")]
    let preferredRoot = "file://CrystalBall/home/ruguer/Videos"
    let expected = ["http://archive/video/Show1/Season 1/Episode1-1.mkv";
                    "file://CrystalBall/home/ruguer/Videos/Show1/Season 1/Episode1-1.mkv";
                    "ftp://archive/volume1/video/Show1/Season 1/Episode1-1.mkv"]
    let actual = correctLocations preferredRoot input
    Assert.Equal<string list>(expected, actual)

[<Fact>]
let ``Two locations of three wrong``() =
    let input = [("http://archive/video","/Show1/Season 1/Episode1-1.");
                    ("file://CrystalBall/home/ruguer/Videos","/Show1/Season 1/Episode1-1.mkv");
                    ("ftp://archive/volume1/video","/Show1//Episode1-1.mkv")]
    let preferredRoot = "file://CrystalBall/home/ruguer/Videos"
    let expected = ["http://archive/video/Show1/Season 1/Episode1-1.mkv";
                    "file://CrystalBall/home/ruguer/Videos/Show1/Season 1/Episode1-1.mkv";
                    "ftp://archive/volume1/video/Show1/Season 1/Episode1-1.mkv"]
    let actual = correctLocations preferredRoot input
    Assert.Equal<string list>(expected, actual)

[<Fact>]
let ``One of two locations wrong``() =
    let input = [("http://archive/video","/Show1/Sason 1/Episode-2-2.mkv");
                    ("file://CrystalBall/home/ruguer/Videos","/Show1/Season 1/Episode1-1.mkv")]
    let preferredRoot = "file://CrystalBall/home/ruguer/Videos"
    let expected = ["http://archive/video/Show1/Season 1/Episode1-1.mkv";
                    "file://CrystalBall/home/ruguer/Videos/Show1/Season 1/Episode1-1.mkv"]
    let actual = correctLocations preferredRoot input
    Assert.Equal<string list>(expected, actual)