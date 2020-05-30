using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using ExceptionStorage.ExceptionModels;

namespace ExceptionStorage.Controllers
{

   
    [ApiController]
    [Route("[controller]")]
    public class TestController : ControllerBase
    {

        private readonly ILogger<TestController> _logger;

        public TestController(ILogger<TestController> logger)
        {
            _logger = logger;
        }

        [HttpGet("{name}")]
        public IResult Get(string name)
        {
            using (var context = new exceptionContext())
            {
                var records = context.Tests
                    .Select(obj => new TestIdFree {
                        Name = obj.Name,
                        Cron = obj.Cron
                    })
                    .Where(s => s.Name == name)
                    .ToList<TestIdFree>();
                if (!records.Any())
                {
                    return new Result1
                    {
                        status = "ok"
                    };
                }
                else
                {
                    return new Result<TestIdFree>
                    {
                        status = "ok",
                        results = records
                    };
                }
            }
        }


        [HttpPost("{name}")]
        public Result1 Post(string name, [FromBody] Tests obj)
        {
            using (var context = new exceptionContext())
            {
                
                if (obj.Name == name)
                {
                    context.Add(obj);
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
        public Result<TestIdFree> CatchAll()
            {
                 using (var context = new exceptionContext())
            {
                 var records = context.Tests
                    .Select(obj => new TestIdFree {
                        Name = obj.Name,
                        Cron = obj.Cron
                    })
                    .ToList<TestIdFree>();
                return new Result<TestIdFree> {
                    status = "ok",
                    results = records
                };
            }
            }
    }
}