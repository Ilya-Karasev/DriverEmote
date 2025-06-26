package com.example.driveremote
import com.example.driveremote.models.Driver
import com.example.driveremote.models.Manager
import com.example.driveremote.models.Post
import com.example.driveremote.models.Request
import com.example.driveremote.models.Results
import com.example.driveremote.models.User
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
class UnitTest {
    private lateinit var fakeUserDao: TestUser
    private lateinit var apiService: TestUserService
    private lateinit var resultsDao: TestResults
    private lateinit var resultsService: TestResultsService
    private lateinit var requestDao: TestRequest
    private lateinit var requestService: TestRequestService
    private lateinit var driverDao: TestDriver
    private lateinit var driverService: TestDriverService
    private lateinit var managerDao: TestManager
    private lateinit var managerService: TestManagerService

    @Before
    fun setup() {
        fakeUserDao = TestUser()
        apiService = TestUserService()
        resultsDao = TestResults()
        resultsService = TestResultsService()
        requestDao = TestRequest()
        requestService = TestRequestService()
        driverDao = TestDriver()
        driverService = TestDriverService()
        managerDao = TestManager()
        managerService = TestManagerService()
    }

    // ------------- Тесты для TestUser ----------------
    @Test
    fun insertUser_and_getAllUsers_returnsCorrectUser() = runBlocking {
        val user = createTestUser()
        fakeUserDao.insertUser(user)

        val users = fakeUserDao.getAllUsers()
        assertEquals(1, users.size)
        assertEquals("Иванов", users[0].surName)
    }

    @Test
    fun getUserByEmailAndPassword_returnsUserIfExists() = runBlocking {
        val user = createTestUser()
        fakeUserDao.insertUser(user)

        val result = fakeUserDao.getUserByEmailAndPassword("ivanov@example.com", "123456")
        assertNotNull(result)
        assertEquals("Иванов", result?.surName)
    }

    @Test
    fun getUserById_returnsCorrectUser() = runBlocking {
        val user = createTestUser().copy(id = 1)
        fakeUserDao.insertUser(user)

        val result = fakeUserDao.getUserById(1)
        assertNotNull(result)
        assertEquals(1, result?.id)
    }

    // ------------- Тесты для TestService ----------------
    @Test
    fun createUser_and_getUserById_returnsSameUser() = runBlocking {
        val user = createTestUser().copy(id = 10)
        apiService.createUser(user)

        val result = apiService.getUserById(10)
        assertEquals("Иванов", result.surName)
    }

    @Test
    fun loginUser_successfulLogin_returnsUser() = runBlocking {
        val user = createTestUser()
        apiService.createUser(user)

        val result = apiService.loginUser("ivanov@example.com", "123456")
        assertNotNull(result)
        assertEquals("Иванов", result?.surName)
    }

    // ------------- Тесты для TestResultsDao ----------------
    @Test
    fun insertResult_and_getResultsByUser_returnsCorrectResults() = runBlocking {
        val result = createTestResult(userId = 1)
        resultsDao.insertResult(result)

        val list = resultsDao.getResultsByUser(1)
        assertEquals(1, list.size)
        assertEquals("Внимание", list[0].status)
    }

    @Test
    fun getLastResultByUser_returnsMostRecentResult() = runBlocking {
        resultsDao.insertResult(createTestResult(userId = 1, testDate = "01.06.2025 — 08:00"))
        resultsDao.insertResult(createTestResult(userId = 1, testDate = "02.06.2025 — 08:00"))

        val last = resultsDao.getLastResultByUser(1)
        assertEquals("02.06.2025 — 08:00", last?.testDate)
    }

    // ------------- Тесты для TestResultsService ----------------
    @Test
    fun addResult_and_getLastResult_returnsSameResult() = runBlocking {
        val result = createTestResult(userId = 1, testDate = "03.06.2025 — 10:00")
        resultsService.addResult(result)

        val fetched = resultsService.getLastResultByUser(1)
        assertEquals("03.06.2025 — 10:00", fetched?.testDate)
    }

    @Test
    fun getResultsByUser_returnsAllUserResults() = runBlocking {
        resultsService.addResult(createTestResult(userId = 2, testDate = "01.06.2025 — 08:00"))
        resultsService.addResult(createTestResult(userId = 2, testDate = "02.06.2025 — 08:00"))

        val list = resultsService.getResultsByUser(2)
        assertEquals(2, list.size)
    }

    // ------------- Тесты для TestRequestDao ----------------
    @Test
    fun insertRequest_and_getAllRequests_returnsCorrectRequest() = runBlocking {
        val request = Request(id = 0, sender = 1, receiver = 2)
        requestDao.insertRequest(request)

        val all = requestDao.getAllRequests()
        assertEquals(1, all.size)
        assertEquals(1, all[0].sender)
        assertEquals(2, all[0].receiver)
    }

    @Test
    fun getRequestsForReceiver_returnsOnlyMatching() = runBlocking {
        requestDao.insertRequest(Request(sender = 1, receiver = 2))
        requestDao.insertRequest(Request(sender = 3, receiver = 2))
        requestDao.insertRequest(Request(sender = 4, receiver = 5))

        val received = requestDao.getRequestsForReceiver(2)
        assertEquals(2, received.size)
    }

    @Test
    fun getRequestsForSender_returnsOnlyMatching() = runBlocking {
        requestDao.insertRequest(Request(sender = 10, receiver = 2))
        requestDao.insertRequest(Request(sender = 10, receiver = 3))
        requestDao.insertRequest(Request(sender = 11, receiver = 4))

        val sent = requestDao.getRequestsForSender(10)
        assertEquals(2, sent.size)
    }

    // ------------- Тесты для TestRequestService ----------------
    @Test
    fun createRequest_and_getAllRequests_returnsSame() = runBlocking {
        val created = requestService.createRequest(Request(sender = 1, receiver = 2))

        val all = requestService.getAllRequests()
        assertEquals(1, all.size)
        assertEquals(created, all[0])
    }

    @Test
    fun deleteRequest_removesCorrectRequest() = runBlocking {
        val req1 = requestService.createRequest(Request(sender = 1, receiver = 2))
        val req2 = requestService.createRequest(Request(sender = 3, receiver = 4))

        requestService.deleteRequest(req1.id)
        val remaining = requestService.getAllRequests()

        assertEquals(1, remaining.size)
        assertEquals(req2.id, remaining[0].id)
    }

    @Test
    fun getRequestsByReceiver_returnsFilteredList() = runBlocking {
        requestService.createRequest(Request(sender = 1, receiver = 5))
        requestService.createRequest(Request(sender = 2, receiver = 5))
        requestService.createRequest(Request(sender = 3, receiver = 6))

        val list = requestService.getRequestsByReceiver(5)
        assertEquals(2, list.size)
    }

    // ------------- Тесты для TestDriverDao ----------------
    @Test
    fun insertDriver_and_getById_returnsCorrectDriver() = runBlocking {
        val driver = createTestDriver(id = 1)
        driverDao.insertDriver(driver)

        val result = driverDao.getDriverById(1)
        assertNotNull(result)
        assertEquals(false, result?.isCompleted)
        assertEquals(1, result?.id)
    }

    @Test
    fun updateCompletionStatus_setsCorrectValue() = runBlocking {
        val driver = createTestDriver(id = 2)
        driverDao.insertDriver(driver)

        driverDao.updateCompletionStatus(2, true)
        val updated = driverDao.getDriverById(2)
        assertEquals(true, updated?.isCompleted)
    }

    @Test
    fun updateQuantity_setsCorrectQuantity() = runBlocking {
        val driver = createTestDriver(id = 3, quantity = 1)
        driverDao.insertDriver(driver)

        driverDao.updateQuantity(3, 2)
        val updated = driverDao.getDriverById(3)
        assertEquals(2, updated?.quantity)
    }

    @Test
    fun updateStatus_setsCorrectStatus() = runBlocking {
        val driver = createTestDriver(id = 4)
        driverDao.insertDriver(driver)

        driverDao.updateStatus(4, "Повышен риск")
        val updated = driverDao.getDriverById(4)
        assertEquals("Повышен риск", updated?.status)
    }

    // ------------- Тесты для TestDriverService ----------------
    @Test
    fun saveDriver_and_getById_returnsSameDriver() = runBlocking {
        val driver = createTestDriver(id = 100)
        driverService.saveDriver(driver)

        val result = driverService.getDriverById(100)
        assertEquals(driver.id, result.id)
        assertEquals(driver.status, result.status)
    }

    @Test
    fun updateDriver_updatesFieldsCorrectly() = runBlocking {
        val driver = createTestDriver(id = 200, quantity = 1)
        driverService.saveDriver(driver)

        val updated = driver.copy(quantity = 2, status = "Критический")
        driverService.updateDriver(200, updated)

        val result = driverService.getDriverById(200)
        assertEquals(2, result.quantity)
        assertEquals("Критический", result.status)
    }

    // ------------- Тесты для TestManagerDao ----------------
    @Test
    fun insertManager_and_getById_returnsCorrectManager() = runBlocking {
        val manager = Manager(id = 1, employeesList = listOf(10, 20, 30))
        managerDao.insertManager(manager)

        val fetched = managerDao.getManagerById(1)
        assertNotNull(fetched)
        assertEquals(3, fetched?.employeesList?.size)
        assertTrue(fetched?.employeesList?.contains(10) == true)
    }

    @Test
    fun updateEmployeesList_correctlyUpdatesList() = runBlocking {
        val manager = Manager(id = 2, employeesList = listOf(1, 2))
        managerDao.insertManager(manager)

        managerDao.updateEmployees(2, listOf(3, 4, 5))
        val updated = managerDao.getManagerById(2)
        assertEquals(listOf(3, 4, 5), updated?.employeesList)
    }

    @Test
    fun getUsersByIds_returnsCorrectUsers() = runBlocking {
        val user1 = createTestUser().copy(id = 10)
        val user2 = createTestUser().copy(id = 20)
        fakeUserDao.insertUser(user1)
        fakeUserDao.insertUser(user2)

        val result = managerDao.getUsersByIds(listOf(10, 20))
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == 10 })
        assertTrue(result.any { it.id == 20 })
    }

    // ------------- Тесты для TestManagerService ----------------
    @Test
    fun saveManager_and_getById_returnsSameManager() = runBlocking {
        val manager = Manager(id = 3, employeesList = listOf(100, 101))
        managerService.saveManager(manager)

        val fetched = managerService.getManagerById(3)
        assertNotNull(fetched)
        if (fetched != null) {
            assertEquals(manager.employeesList, fetched.employeesList)
        }
    }

    @Test
    fun updateEmployeesList_correctlyChangesList() = runBlocking {
        val manager = Manager(id = 4, employeesList = listOf(1, 2))
        managerService.saveManager(manager)

        managerService.updateEmployeesList(4, listOf(7, 8))
        val updated = managerService.getManagerById(4)
        if (updated != null) {
            assertEquals(listOf(7, 8), updated.employeesList)
        }
    }

    // ---------------- Вспомогательные методы и классы ----------------
    private fun createTestUser(): User {
        return User(
            id = 0,
            surName = "Иванов",
            firstName = "Иван",
            fatherName = "Иванович",
            age = 35,
            post = Post.ВОДИТЕЛЬ,
            email = "ivanov@example.com",
            password = "123456"
        )
    }

    private fun createTestResult(userId: Int, testDate: String = "01.06.2025 — 08:00"): Results {
        return Results(
            id = 0,
            userId = userId,
            testDate = testDate,
            emotionalExhaustionScore = 20,
            depersonalizationScore = 30,
            personalAchievementScore = 25,
            totalScore = 75
        )
    }

    private fun createTestDriver(id: Int, quantity: Int = 1): Driver {
        return Driver(
            id = id,
            isCompleted = false,
            testingTime = listOf("08:00", "20:00"),
            quantity = quantity,
            status = "Норма"
        )
    }
}