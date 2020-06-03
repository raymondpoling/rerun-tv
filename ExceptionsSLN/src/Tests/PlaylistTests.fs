module PlaylistTests

open System
open Xunit
open Library.TestPlaylist
open TheseTypes.TestResults
open Newtonsoft.Json.Linq


[<Fact>]
let ``Valid playlist?``() =
    let input = new JObject(
        new JProperty("status",new JValue("ok")),
        new JProperty("catalog-ids",new JArray(new JValue("TEST0101001"),new JValue("TEST0101002"),new JValue("TEST0101003"))))
    let verifier _ = true
    let actual = catalogIds input verifier
    Assert.Equal(actual, Success)

[<Fact>]
let ``Inalid playlist?``() =
    let input = new JObject(
        new JProperty("status",new JValue("failed")),
        new JProperty("catalog-ids",new JArray(new JValue("TEST0101001"),new JValue("TEST0101002"),new JValue("TEST0101003"))))
    let verifier _ = false
    let actual = catalogIds input verifier
    Assert.Equal(actual, Failures(Message "Catalog ID does not exist", Errors ["TEST0101001";"TEST0101002";"TEST0101003"]))

[<Fact>]
let ``Partial failure?``() =
    let input = new JObject(
        new JProperty("status",new JValue("failed")),
        new JProperty("catalog-ids",new JArray(new JValue("TEST0101001"),new JValue("TEST0101002"),new JValue("TEST0101003"))))
    let verifier (s : string) = not (s.Contains("002"))
    let actual = catalogIds input verifier
    Assert.Equal(actual, Failures(Message "Catalog ID does not exist", Errors ["TEST0101002"]))