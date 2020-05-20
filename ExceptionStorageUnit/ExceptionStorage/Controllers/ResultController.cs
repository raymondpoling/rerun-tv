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
            Console.WriteLine("Looking for: " + name);
            using (var context = new exceptionContext())
            {
                var records = context.Results
                    .Where(s => s.Test.Name == name)
                    .Include(s => s.Test)
                    .Select(s => new ResultIdFree {
                        Test = new TestIdFree {
                            Name = s.Test.Name,
                            Cron = s.Test.Cron
                        },
                        Date = s.Date,
                        PassFail = s.PassFail > 0,
                        RemediationSucceeded = s.RemediationSucceeded > 0,
                        StatusMessage = s.StatusMessage
                    })
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
            using (var context = new exceptionContext())
            {
                Console.WriteLine("Name: " + obj.StatusMessage);
                
                if (obj.Test.Name == name)
                {
                    var test = context.Tests
                        .Where(s => s.Name == name)
                        .First();
                    context.Add(new Results {
                        Date = obj.Date,
                        Test = test,
                        PassFail = obj.PassFail ? (byte)1 : (byte)0,
                        RemediationSucceeded = obj.RemediationSucceeded ? (byte)1 : (byte)0,
                        StatusMessage = obj.StatusMessage
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
    }
}