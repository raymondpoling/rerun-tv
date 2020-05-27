using System;
using Xunit;
using ExceptionStorage.ExceptionModels;
using System.Linq;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata;

[assembly: CollectionBehavior(DisableTestParallelization = true)]
namespace ExceptionStorageTest
{
    public class DBTest
    {
        [Fact]
        public void CanSaveTest()
        {
            var model = new Tests {
                Id = 0,
                Name = "Test1",
                Cron = "* * * * *"
            };
            using(var context = new exceptionContext()) {
                context.Tests.Add(model);
                context.SaveChanges();
                Assert.True(true);
            }        
        }
        [Fact]
        public void CanFindSavedDataTest()
        {
            var model = new Tests {
                Name = "Test2",
                Cron = "* * * * *"
            };
            using(var context = new exceptionContext()) {
                context.Tests.Add(model);
                context.SaveChanges();
            } 
            using(var context = new exceptionContext()) {
                var record = context.Tests
                .Where(s => s.Name == "Test2")
                .First<Tests>();
                Assert.Equal("Test2", record.Name);
                Assert.True(record.Id > 0, "id is not greater than zero!");
            }    
        }
    }
    public class DBResult {
           [Fact]
     public void CanSaveResultsTest()
     {
         using(var context = new exceptionContext()) {
             var test = new Tests {
                 Name = "Test3",
                 Cron = "* * * * *"
             };
             context.Tests.Add(test);
             var result = new Results {
                Test = test,
                Date = DateTime.Now,
                PassFail = 1,
                RemediationSucceeded = 1,
                StatusMessage = "This is the third test"
            };
            context.Results.Add(result);
            context.SaveChanges();
            Assert.True(true);
         }
     }
     [Fact]
     public void CanRetrieveResulstsTest()
     {
         using(var context = new exceptionContext()) {
             var test = new Tests {
                 Name = "Test4",
                 Cron = "* * * * *"
             };
             context.Tests.Add(test);
             var result = new Results {
                Test = test,
                Date = DateTime.Now,
                PassFail = 1,
                RemediationSucceeded = 1,
                StatusMessage = "This is the fourth test"
            };
            context.Results.Add(result);
            context.SaveChanges();
         }
         using(var context = new exceptionContext()) {
             var result = context.Results
             .Where(s => s.StatusMessage == "This is the fourth Test")
             .Include(s => s.Test)
             .First();
             Assert.True(result.Test.Name == "Test4");
             Assert.True(result.PassFail == 1);
             Assert.True(result.RemediationSucceeded == 1);
         }
     }

        [Fact]
        public void CanFindLatestExecutionTest()
        {
             using(var context = new exceptionContext()) {
             var test = new Tests {
                 Name = "Test5",
                 Cron = "* * * * *"
             };
             context.Tests.Add(test);
             context.Add(new Results {
                Test = test,
                Date = DateTime.Parse("7/16/2008 8:32:45.126 AM"),
                PassFail = 1,
                RemediationSucceeded = 1,
                StatusMessage = "This is the fifth test"
            });
            context.Results.Add(new Results {
                Test = test,
                Date = DateTime.Parse("7/17/2008 8:32:45.126 AM"),
                PassFail = 1,
                RemediationSucceeded = 1,
                StatusMessage = "This is the sixth test"
            });
            context.Results.Add(new Results {
                Test = test,
                Date = DateTime.Parse("7/18/2008 8:32:45.126 AM"),
                PassFail = 1,
                RemediationSucceeded = 1,
                StatusMessage = "This is the seventh test"
            });
            context.SaveChanges();
         }
         using(var context = new exceptionContext()) {
             var result = context.Results
             .OrderBy(s => s.Id)
             .Include(s => s.Test)
             .Where(s => s.Test.Name == "Test5")
             .OrderByDescending(s => s.Date)
             .First();
             
             Assert.True(result.Test.Name == "Test5");
             Assert.True(result.StatusMessage == "This is the seventh test", "Test 5 Failed, got: " + result.StatusMessage);
             Assert.True(result.PassFail == 1);
             Assert.True(result.RemediationSucceeded == 1);
         }
        }
        [Fact]
        public void CanFindLastPassExecutionTest()
        {
             using(var context = new exceptionContext()) {
             var test = new Tests {
                 Name = "Test6",
                 Cron = "* * * * *"
             };
             context.Tests.Add(test);
             context.Add(new Results {
                Test = test,
                Date = DateTime.Parse("7/16/2008 8:32:45.126 AM"),
                PassFail = 0,
                RemediationSucceeded = 1,
                StatusMessage = "This is the eigth test"
            });
            context.Results.Add(new Results {
                Test = test,
                Date = DateTime.Parse("7/17/2008 8:32:45.126 AM"),
                PassFail = 1,
                RemediationSucceeded = 1,
                StatusMessage = "This is the ninth test"
            });
            context.Results.Add(new Results {
                Test = test,
                Date = DateTime.Parse("7/18/2008 8:32:45.126 AM"),
                PassFail = 0,
                RemediationSucceeded = 1,
                StatusMessage = "This is the tenth test"
            });
            context.SaveChanges();
         }
         using(var context = new exceptionContext()) {
             var result = context.Results
             .OrderByDescending(s => s.Date)
             .Include(s => s.Test)
             .Where(s => s.Test.Name == "Test6" && s.PassFail > 0)
             .First();
             Assert.True(result.Test.Name == "Test6");
             Assert.True(result.StatusMessage == "This is the ninth test", "Test 6 Failed, got: " + result.StatusMessage);
             Assert.True(result.PassFail == 1);
             Assert.True(result.RemediationSucceeded == 1);
         }
        }
        [Fact]
        public void CanFindLastFailExecutionTest()
        {
           using(var context = new exceptionContext()) {
             var test = new Tests {
                 Name = "Test7",
                 Cron = "* * * * *"
             };
             context.Tests.Add(test);
             context.Add(new Results {
                Test = test,
                Date = DateTime.Parse("7/16/2008 8:32:45.126 AM"),
                PassFail = 0,
                RemediationSucceeded = 1,
                StatusMessage = "This is the 11 test"
            });
            context.Results.Add(new Results {
                Test = test,
                Date = DateTime.Parse("7/17/2008 8:32:45.126 AM"),
                PassFail = 1,
                RemediationSucceeded = 1,
                StatusMessage = "This is the 12 test"
            });
            context.Results.Add(new Results {
                Test = test,
                Date = DateTime.Parse("7/18/2008 8:32:45.126 AM"),
                PassFail = 0,
                RemediationSucceeded = 1,
                StatusMessage = "This is the 13 test"
            });
            context.SaveChanges();
         }
         using(var context = new exceptionContext()) {
             var result = context.Results
             .OrderByDescending(s => s.Date)
             .Include(s => s.Test)
             .Where(s => s.Test.Name == "Test7" && s.PassFail == 0)
             .First();
             Assert.True(result.Test.Name == "Test7");
             Assert.True(result.StatusMessage == "This is the 13 test", "Test 7 Failed, got: " + result.StatusMessage);
             Assert.True(result.PassFail == 0);
             Assert.True(result.RemediationSucceeded == 1);
         }
        }
    }
}
