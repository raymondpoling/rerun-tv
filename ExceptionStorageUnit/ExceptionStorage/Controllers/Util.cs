using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using ExceptionStorage.ExceptionModels;

namespace ExceptionStorage.Controllers
{
    public class TestIdFree
    {
        public string Name { get; set; }
        public string Cron { get; set; }
    }

    public class ResultIdFree
    {
        public DateTime Date { get; set; }

        public string Test { get; set; }
        public bool PassFail { get; set; }
        public bool RemediationSucceeded { get; set; }
        public string StatusMessage { get; set; }
        public string Args { get; set; }
    }

    public interface IResult
    {
        public string status { get; set; }
    }

    public class Result<T> : IResult
    {
        public string status { get; set; }
        public List<T> results { get; set; }
    }

    public class Result1 : IResult
    {
        public string status { get; set; }
    }
}