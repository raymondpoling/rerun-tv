using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using ExceptionStorage.ExceptionModels;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata;


namespace ExceptionStorage.Controllers
{
    [ApiController]
    [Route("{*url}", Order = 999)]
    public class NotFoundController : ControllerBase
    {

        public Result1 CatchAll()
        {
            return new Result1
                {
                    status = "not found"
                };
        }
    }

    [ApiController]
    [Route("[controller]")]
    public class ResultController : ControllerBase
    {

        
        private readonly ILogger<ResultController> _logger;

        public ResultController(ILogger<ResultController> logger)
        {
            _logger = logger;
        }

        [HttpGet("{name}")]
        public IResult Get(string name)
        {
            using (var context = new exceptionContext())
            {
                var records = context.Results
                    .Where(s => s.Test.Name == name)
                    .Include(s => s.Test)
                    .Select(s => new ResultIdFree {
                        Test =  s.Test.Name,
                        Date = s.Date,
                        PassFail = s.PassFail > 0,
                        RemediationSucceeded = s.RemediationSucceeded > 0,
                        StatusMessage = s.StatusMessage,
                        Args = s.Args
                    })
                    .OrderByDescending(s => s.Date)
                    .Take(10)
                    .ToList<ResultIdFree>();
                if (!records.Any())
                {
                    return new Result1
                    {
                        status = "ok"
                    };
                }
                else
                {
                    return new Result<ResultIdFree>
                    {
                        status = "ok",
                        results = records
                    };
                }
            }
        }


        [HttpPost("{name}")]
        public Result1 Post(string name, [FromBody] ResultIdFree obj)
        {
            _logger.LogInformation("Object that is being deserialized: " + obj.ToString());
            using (var context = new exceptionContext())
            {
                if (obj.Test == name)
                {
                    var test = context.Tests
                        .Where(s => s.Name == name)
                        .First();
                    context.Add(new Results {
                        Date = obj.Date,
                        Test = test,
                        PassFail = obj.PassFail ? (byte)1 : (byte)0,
                        RemediationSucceeded = obj.RemediationSucceeded ? (byte)1 : (byte)0,
                        StatusMessage = obj.StatusMessage,
                        Args = obj.Args
                    });
                    context.SaveChanges();
                    return new Result1
                    {
                        status = "ok"
                    };
                }
                else
                {
                    return new Result1
                    {
                        status = "failed"
                    };
                }
            }
        }
        [Route("{*url}", Order = 999)]
        public Result1 CatchAll()
            {
                return new Result1
                    {
                        status = "not found"
                    };
            }
    }
}