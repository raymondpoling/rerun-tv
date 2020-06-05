namespace CorrectLibrary

open System
open TheseTypes.TestResults

module LocationCorrection =
    let correctLocations (preferredRoot : string) (locations : (string * string) list) : string list =
        let chosenTail = query {for location in locations do
                                where ((fst location) = preferredRoot)
                                select (snd location)
                                head } 
        locations |> List.map (fun t -> (fst t) + chosenTail)

