using System;
using System.Collections.Generic;

namespace ExceptionStorage.ExceptionModels
{
    public partial class Results
    {
        public long Id { get; set; }
        public DateTime Date { get; set; }
        public long TestId { get; set; }
        public byte PassFail { get; set; }
        public byte RemediationSucceeded { get; set; }
        public string StatusMessage { get; set; }
        public string Args { get; set; }
        public virtual Tests Test { get; set; }
    }
}
