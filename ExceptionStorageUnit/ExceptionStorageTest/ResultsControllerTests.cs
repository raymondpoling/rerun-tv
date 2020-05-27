using System;
using Xunit;
using ExceptionStorage.ExceptionModels;
using Moq;
using System.Linq;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata;
using Microsoft.AspNetCore.TestHost;
using System.Net.Http;
using Microsoft.AspNetCore.Hosting;
using ExceptionStorage;
using Microsoft.Extensions.Logging;
using ExceptionStorage.Controllers;
using Microsoft.AspNetCore.Http;

// [assembly: CollectionBehavior(DisableTestParallelization = true)]
namespace ExceptionStorageTest
{
    public class ResultsControllerTest
    {

        private readonly TestServer _server;
        private readonly HttpClient _client;
        private readonly Mock<ILogger<ResultController>> _loggerMock;

        private readonly Mock<HttpContext> _contextMock;

        public ResultsControllerTest()
        {
            // Arrange
            _server = new TestServer(new WebHostBuilder()
               .UseStartup<Startup>());
            _client = _server.CreateClient();
            _loggerMock = new Mock<ILogger<ResultController>>();
            _contextMock = new Mock<HttpContext>();
        }

        [Fact]
        public void PostTest()
        {
            //Arrange
            var test = new TestIdFree
            {
                Name = "Results1Test",
                Cron = "1 1 1 1 1"
            };
            var result = new ResultIdFree {
                Date = DateTime.Now,
                Test = test,
                PassFail = false,
                RemediationSucceeded = false,
                StatusMessage = "Failed to even run"
            };

            //...

            //Act
            var resultController = new ResultController(
                _loggerMock.Object
                );

            var testController = new TestController(
                new Mock<ILogger<TestController>>().Object
                );


            resultController.ControllerContext.HttpContext = _contextMock.Object;
            testController.ControllerContext.HttpContext = _contextMock.Object;

            testController.Post(test.Name, new Tests{Name = test.Name, Cron = test.Cron});
            var actionResult = resultController.Post(test.Name, result);

            //Assert
            var viewResult = Assert.IsType<Result1>(actionResult);
            Assert.True(viewResult.status == "ok", "Test was not ok!");
        }

        [Fact]
        public void GetTest()
        {
           //Arrange
            var test = new TestIdFree
            {
                Name = "Results2Test",
                Cron = "1 1 1 1 1"
            };
            var result = new ResultIdFree {
                Date = DateTime.Now,
                Test = test,
                PassFail = false,
                RemediationSucceeded = false,
                StatusMessage = "Failed to even run twice"
            };

            //...

            //Act
            var resultController = new ResultController(
                _loggerMock.Object
                );

            var testController = new TestController(
                new Mock<ILogger<TestController>>().Object
                );

            resultController.ControllerContext.HttpContext = _contextMock.Object;
            
            testController.ControllerContext.HttpContext = _contextMock.Object;

            testController.Post(test.Name, new Tests{Name = test.Name, Cron = test.Cron});

            var actionResult = resultController.Post(test.Name, result);
            var actualResult = resultController.Get(test.Name);

            //Assert
            
            var viewResult = Assert.IsType<Result<ResultIdFree>>(actualResult);
            Assert.True(viewResult.status == "ok", "Test was not ok!");
            Assert.True(viewResult.results.First().Test.Name == test.Name, "Wrong name: " + viewResult.results.First().Test.Name);
            Assert.True(viewResult.results.Count == 1, "Wrong Length " + viewResult.results.Count);
        }

        [Fact]
        public void DuplicateResults()
        {
           //Arrange
            var test = new TestIdFree
            {
                Name = "Results3Test",
                Cron = "1 1 1 1 1"
            };
            var result1 = new ResultIdFree {
                Date = DateTime.Now,
                Test = test,
                PassFail = false,
                RemediationSucceeded = false,
                StatusMessage = "Failed to even run twice"
            };
            var result2 = new ResultIdFree {
                Date = DateTime.Now,
                Test = test,
                PassFail = false,
                RemediationSucceeded = false,
                StatusMessage = "Failed to even run twice"
            };
            var result3 = new ResultIdFree {
                Date = DateTime.Now,
                Test = test,
                PassFail = false,
                RemediationSucceeded = false,
                StatusMessage = "Failed to even run twice"
            };

            //...
            var testController = new TestController(
                new Mock<ILogger<TestController>>().Object
                );
            //Act
            var resultController = new ResultController(
                _loggerMock.Object
                );

            testController.ControllerContext.HttpContext = _contextMock.Object;

            testController.Post(test.Name, new Tests{Name = test.Name, Cron = test.Cron});

            resultController.ControllerContext.HttpContext = _contextMock.Object;
            resultController.Post(test.Name, result1);
            resultController.Post(test.Name, result2);
            resultController.Post(test.Name, result3);

            var actualResult = resultController.Get(test.Name);

            //Assert
            
            var viewResult = Assert.IsType<Result<ResultIdFree>>(actualResult);
            Assert.True(viewResult.status == "ok", "Test was not ok!");
            Assert.True(viewResult.results.First().Test.Name == test.Name, "Wrong name: " + viewResult.results.First().Test.Name);
            Assert.True(viewResult.results.Count == 3, "Wrong Length " + viewResult.results.Count);
        }
    }
}