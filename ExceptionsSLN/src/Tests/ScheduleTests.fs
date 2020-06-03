module ScheduleTests

open System
open Xunit
open Library.TestSchedule
open TheseTypes.TestResults
open Newtonsoft.Json.Linq


[<Fact>]
let ``Valid schedule?``() = 
    let input = new JObject(
                    new JProperty("status",new JValue("ok")),
                    new JProperty("messages",new JArray(new JValue("Does not exist"))))
    let actual = schedule input
    Assert.Equal(actual, Success)

[<Fact>]
let ``Inalid schedule?``() = 
    let input = new JObject(
                    new JProperty("status",new JValue("failed")),
                    new JProperty("messages",new JArray(new JValue("Does not exist"))))
    let actual = schedule input
    Assert.Equal(actual, Failures(Message "Does not exist", Errors ["Does not exist"]))