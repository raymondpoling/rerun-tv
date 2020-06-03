namespace TheseTypes

module TestResults =
    type Message = Message of string
    type Errors = Errors of string list
    type Result =
        | Success
        | Failures of Message * Errors
