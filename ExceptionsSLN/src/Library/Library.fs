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
        let ids = playlist.Value<JArray>("catalog-ids")
                    .Values<JToken>()
                    .Select(fun s -> s.Value<string>())
                    .Where(fun t -> not (verifier t))
        match List.ofSeq(ids) with
            | [] -> 
                Success
            | t -> 
                Failures(Message catalogIdDoesNotExist, Errors t)

module LocationComparison =
    let private matchRoot (roots : string list) (locations : string list) : seq<string * seq<string>> =
        locations |> Seq.groupBy(fun t -> roots.Where(fun s -> (t.Contains(s))).First())
    let private allRoots (roots : string list) (groups : seq<string * seq<string>>) = 
        roots.Length = groups.Count()
    let private oneForEach (groups : seq<string * seq<string>>) = 
        let checkGroups = List.ofSeq(groups) |> List.filter(fun t -> (snd t).Count() = 1)
        groups.Count() = checkGroups.Length
    let private withoutRoot (item : string * seq<string>)=
        let (root,aList) = item
        aList.Select(fun s -> (new Regex(root)).Replace(s,"")).First()
    let private rootlessList (aList : seq<string * seq<string>>) = 
        aList.Select(fun s -> withoutRoot(s))
    let private allRootlessEqual aList = match aList with
        | hd::tl -> tl |> List.forall(fun s -> s = hd)
    let private doesNotMatch = "Files do not match."
    let private missingRoots = "Mismatched roots."
    let compareLocations (roots : string list) (locations : JArray) : Result =
        let listLocations = List.ofSeq(locations.Select(fun u -> u.Value<string>()).ToList<string>())
        let errors = List.ofSeq(listLocations)
        let groups = matchRoot roots listLocations
        let rootless = List.ofSeq(rootlessList groups)
        if(not (oneForEach groups)) then
            Failures(Message missingRoots, Errors errors)
        else if(not (allRoots roots groups)) then
            Failures(Message missingRoots, Errors errors)
        else if(not (allRootlessEqual rootless)) then
            Failures(Message doesNotMatch, Errors errors)
        else
            Success