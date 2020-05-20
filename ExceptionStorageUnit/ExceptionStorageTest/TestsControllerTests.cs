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
    public class TestsControllerTest
    {

        private readonly TestServer _server;
        private readonly HttpClient _client;
        private readonly Mock<ILogger<TestController>> _loggerMock;

        private readonly Mock<HttpContext> _contextMock;

        public TestsControllerTest()
        {
            // Arrange
            _server = new TestServer(new WebHostBuilder()
               .UseStartup<Startup>());
            _client = _server.CreateClient();
            _loggerMock = new Mock<ILogger<TestController>>();
            _contextMock = new Mock<HttpContext>();
        }

        [Fact]
        public void PostTest()
        {
            //Arrange
            var testName = "PostTest1";
            var cron = "* * * * *";

            //...

            //Act
            var testController = new TestController(
                _loggerMock.Object
                );

            testController.ControllerContext.HttpContext = _contextMock.Object;
            var actionResult = testController.Post(testName, new Tests { Name = testName, Cron = cron });

            //Assert
            var viewResult = Assert.IsType<Result1>(actionResult);
            Console.WriteLine("X: " + viewResult);
            Assert.True(viewResult.status == "ok", "Test was not ok!");
        }

        [Fact]
        public void GetTest()
        {
            //Arrange
            var testName = "GetTest1";
            var cron = "* * 5 * *";

            //...

            //Act
            var testController = new TestController(
                _loggerMock.Object
                );

            testController.ControllerContext.HttpContext = _contextMock.Object;
            var actionResult = testController.Post(testName, new Tests { Name = testName, Cron = cron });
            var actualResult = testController.Get(testName);

            //Assert
            Console.WriteLine("XXXXXXXXXXXXX: " + actualResult);
            
            var viewResult = Assert.IsType<Result<TestIdFree>>(actualResult);
            Console.WriteLine("X2222222222222: " + viewResult);
            Assert.True(viewResult.status == "ok", "Test was not ok!");
            Assert.True(viewResult.results.First().Name == testName, "Wrong name: " + viewResult.results.First().Name);
        }
    }
}