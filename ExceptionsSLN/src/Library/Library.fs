namespace Library

open Newtonsoft.Json.Linq
open System
open System.Linq
open System.Text.RegularExpressions
open TheseTypes.TestResults

module TestSchedule =
    let schedule (response : JObject) =
        match response.Value<string>("status") = "ok" with
            | true -> 
                Success
            | _ -> 
                let messages = response.Value<JArray>("messages").Select(fun s -> s.Value<String>())
                let messageString = String.Join(", ", messages)
                let messageList = List.ofSeq(messages)
                Failures(Message messageString, Errors messageList)

module TestPlaylist =
    let private catalogIdDoesNotExist = "Catalog ID does not exist"
    let catalogIds (playlist : JObject) (verifier : string -> bool) =
        let ids = query { for id in playlist.Value<JArray>("catalog-ids").Values<JToken>() do
                            where (not (verifier (id.Value<string>())))
                            select (id.Value<string>())} |> List.ofSeq
        if(List.isEmpty(ids)) then
            Success
        else
            Failures(Message catalogIdDoesNotExist, Errors ids)

module LocationComparison =
    let private matchRoot (roots : string seq) (locations : string seq) = 
        query { for root in roots do
                for location in locations do
                where  (location.StartsWith(root))
                groupValBy location root into u
                select (u.Key,(seq u) |> Seq.map (fun t -> t.Replace(u.Key,"")))}
    let private allRoots (roots : string seq) (groups : 'a seq) = 
        (roots.Count()) = (groups.Count())
    let private oneForEach (groups : seq<string * seq<string>>) = 
        let checkGroups = List.ofSeq(groups) |> List.filter(fun t -> (snd t).Count() = 1)
        groups.Count() = checkGroups.Length
    let private matchRootless (groups : seq<string * seq<string>>) =
        let toCheck = groups.Select(fun t -> snd t).SelectMany(fun u -> u)
        let first = toCheck.First()
        toCheck |> Seq.forall(fun u -> first = u)
    let private doesNotMatch = "Files do not match."
    let private missingRoots = "Mismatched roots."
    let compareLocations (roots : string list) (locations : JArray) : Result =
        let listLocations = query {for location in locations do
                                   select (location.Value<string>()) } 
        let errors = listLocations |> List.ofSeq
        let groups = matchRoot roots listLocations
        if(not (oneForEach groups)) then
            Failures(Message missingRoots, Errors errors)
        elif(not (allRoots roots groups)) then
            Failures(Message missingRoots, Errors errors)
        elif(not (matchRootless groups)) then
            Failures(Message doesNotMatch, Errors errors)
        else
            Success