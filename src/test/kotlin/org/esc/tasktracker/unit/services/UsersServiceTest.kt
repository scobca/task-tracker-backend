package org.esc.tasktracker.unit.services

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.esc.tasktracker.dto.filters.UsersFilterDto
import org.esc.tasktracker.entities.Users
import org.esc.tasktracker.exceptions.DefaultExceptionMessages
import org.esc.tasktracker.exceptions.DoubleRecordException
import org.esc.tasktracker.exceptions.NotFoundException
import org.esc.tasktracker.mappers.UsersMapper
import org.esc.tasktracker.repositories.UsersRepository
import org.esc.tasktracker.repositories.specs.UsersSpecifications
import org.esc.tasktracker.services.UsersService
import org.esc.tasktracker.unit.generators.createUser
import org.esc.tasktracker.unit.generators.createUserCreateDto
import org.esc.tasktracker.unit.generators.createUserUpdateDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UsersServiceTest {
    private val repository = mockk<UsersRepository>()
    private val usersSpecifications = mockk<UsersSpecifications>()
    private val usersMapper = mockk<UsersMapper>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private var defaultExceptionMessages = mockk<DefaultExceptionMessages>()
    private val service = UsersService(repository, usersSpecifications, usersMapper, passwordEncoder, defaultExceptionMessages)
    private lateinit var serviceSpy: UsersService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        serviceSpy = spyk(UsersService(repository, usersSpecifications, usersMapper, passwordEncoder, defaultExceptionMessages))

        every { defaultExceptionMessages.userNotFound() } returns "The user with this ID was not found."
        every { defaultExceptionMessages.userDoubleRecord() } returns "The user with this email already exists."
    }

    @Nested
    inner class GetAllTests {

        val user1 = createUser(name = "Иван Иванов", email = "ivan@example.com")
        val user2 = createUser(name = "Петр Петров", email = "petr@example.com")

        @Test
        fun `getAll with no filters returns all users`() {
            every { usersSpecifications.hasName(any()) } returns null
            every { usersSpecifications.hasEmail(any()) } returns null
            every { repository.findAll(any<Specification<Users>>(), any<Pageable>()) } returns PageImpl(
                listOf(
                    user1,
                    user2
                )
            )

            val result = service.getAll(null, Pageable.unpaged())

            assertEquals(2, result.content.size)
            assertEquals(1, result.totalPages)
            assertTrue { result.isLast }

            assertEquals(user1, result.content[0])
            assertEquals(user2, result.content[1])
        }

        @Test
        fun `getAll with name filter - verifies specification call`() {
            every { usersSpecifications.hasName("Иван") } returns mockk()
            every { usersSpecifications.hasEmail(null) } returns null
            every { repository.findAll(any<Specification<Users>>(), any<Pageable>()) } returns PageImpl(emptyList())

            service.getAll(UsersFilterDto(name = "Иван", email = null), Pageable.unpaged())

            verify { usersSpecifications.hasName("Иван") }
        }

        @Test
        fun `getAll with email filter - verifies specification call`() {
            every { usersSpecifications.hasName(null) } returns null
            every { usersSpecifications.hasEmail("petr@example.com") } returns mockk()
            every { repository.findAll(any<Specification<Users>>(), any<Pageable>()) } returns PageImpl(emptyList())

            service.getAll(UsersFilterDto(name = null, email = "petr@example.com"), Pageable.unpaged())

            verify { usersSpecifications.hasEmail("petr@example.com") }
        }
    }


    @Nested
    inner class GetByIdTests {

        @Test
        fun `should return user by id`() {
            val user1 = createUser()

            every { repository.findById(any<Long>()) } returns Optional.of(user1)

            val result = service.getById(user1.id, true)

            assertEquals(user1, result)
            verify { repository.findById(any()) }
        }

        @Test
        fun `should throw exception when user not found & throwable = true`() {
            every { repository.findById(any<Long>()) } returns Optional.empty()

            val exception = assertFailsWith<NotFoundException> {
                service.getById(id = 1, throwable = true, message = defaultExceptionMessages.userNotFound())
            }

            verify { repository.findById(any()) }
            assertEquals(exception.message, defaultExceptionMessages.userNotFound())
        }

        @Test
        fun `should return null when user not found & throwable = false`() {
            every { repository.findById(any<Long>()) } returns Optional.empty()

            val result = service.getById(id = 1, throwable = false, message = defaultExceptionMessages.userNotFound())

            verify { repository.findById(any()) }
            assertEquals(result, null)
        }
    }

    @Nested
    inner class GetByEmailTests {
        @Test
        fun `should return user by email`() {
            val user1 = createUser()

            every { repository.findByEmail(any()) } returns user1

            val result = service.getByEmail(user1.email)

            verify { repository.findByEmail(user1.email) }
            assertEquals(user1, result)
        }

        @Test
        fun `should throw exception when user not found & throwable = true`() {
            every { repository.findByEmail(any()) } returns null

            val exception = assertFailsWith<NotFoundException> {
                service.getByEmail(email = "test@mail.ru", throwable = true)
            }

            verify { repository.findByEmail(any()) }
            assertEquals(exception.message, defaultExceptionMessages.userNotFound())
        }

        @Test
        fun `should return null when user not found & throwable = false`() {
            every { repository.findByEmail(any()) } returns null

            val result = service.getByEmail(email = "test@mail.ru", throwable = false)

            verify { repository.findByEmail(any()) }
            assertEquals(result, null)
        }
    }

    @Nested
    inner class CreateTests {
        @Test
        fun `should create user`() {
            val dto = createUserCreateDto()
            val user1 = createUser()

            every { service.getByEmail(any(), throwable = false) } returns null
            every { usersMapper.userFromDto(any()) } returns user1
            every { passwordEncoder.encode(any()) } returns "encoded_password"
            every { repository.save(any()) } returns user1

            val result = service.create(dto)

            verify { service.getByEmail(dto.email, throwable = false) }
            verify { usersMapper.userFromDto(dto) }
            verify { passwordEncoder.encode(dto.password) }
            verify { repository.save(user1.copy(password = "encoded_password")) }

            assertEquals(user1, result)
        }

        @Test
        fun `should throw exception when user with the same email already exists`() {
            val dto = createUserCreateDto()
            val user1 = createUser()

            every { service.getByEmail(any(), throwable = false) } returns user1

            val exception = assertFailsWith<DoubleRecordException> {
                service.create(dto)
            }

            verify { service.getByEmail(dto.email, throwable = false) }
            verify(exactly = 0) { usersMapper.userFromDto(any()) }
            verify(exactly = 0) { passwordEncoder.encode(any()) }
            verify(exactly = 0) { repository.save(any()) }

            assertEquals(exception.message, defaultExceptionMessages.userDoubleRecord())
        }
    }

    @Nested
    inner class UpdateTests {
        @Test
        fun `should update only name`() {
            val user = createUser(id = 1L, name = "Old Name")
            val dto = createUserUpdateDto(id = 1L, name = "New Name")

            every { serviceSpy.getById(dto.id, any(), any()) } returns user
            every { serviceSpy.getByEmail(any(), throwable = false) } returns null
            every { repository.save(user) } returns user.copy(name = "New Name")

            val result = serviceSpy.update(dto)

            verify { serviceSpy.getById(dto.id, any(), any()) }
            verify { repository.save(user) }
            verify(exactly = 0) { serviceSpy.getByEmail(any(), any()) }

            assertEquals("New Name", result.name)
        }

        @Test
        fun `should update only email (unique)`() {
            val user = createUser(id = 1, name = "Old name", email = "old@example.com")
            val dto = createUserUpdateDto(id = 1, email = "new@example.com")

            every { serviceSpy.getById(dto.id, any(), any()) } returns user
            every { serviceSpy.getByEmail("new@example.com", throwable = false) } returns null
            every { repository.save(user) } returns user.copy(email = "new@example.com")

            val result = serviceSpy.update(dto)

            verify { serviceSpy.getById(dto.id, any(), any()) }
            verify { serviceSpy.getByEmail("new@example.com", throwable = false) }
            verify { repository.save(user) }

            assertEquals("new@example.com", result.email)
            assertEquals("Old name", result.name)
        }

        @Test
        fun `should throw DoubleRecordException on email conflict`() {
            val dto = createUserUpdateDto(id = 1, email = "busy@example.com")
            val user = createUser(id = 1)

            every { serviceSpy.getById(dto.id, any(), any()) } returns user
            every { serviceSpy.getByEmail("busy@example.com", throwable = false) } returns createUser()

            assertThrows<DoubleRecordException> {
                serviceSpy.update(dto)
            }

            verify { serviceSpy.getById(dto.id, any(), any()) }
            verify { serviceSpy.getByEmail(dto.email!!, throwable = false) }
            verify(exactly = 0) { repository.save(any()) }
        }

        @Test
        fun `should update name and email`() {
            val dto = createUserUpdateDto(id = 1, name = "New name", email = "new@example.com")
            val user = createUser(id = 1, name = "Old name", email = "old@example.com")

            every { serviceSpy.getById(dto.id, any(), any()) } returns user
            every { serviceSpy.getByEmail("new@example.com", throwable = false) } returns null
            every { repository.save(any()) } returns user.copy(
                name = "New Name",
                email = "new@example.com"
            )

            val result = serviceSpy.update(dto)

            verify {
                serviceSpy.getById(dto.id, any(), any())
                serviceSpy.getByEmail("new@example.com", throwable = false)
                repository.save(user)
            }

            assertEquals("New Name", result.name)
            assertEquals("new@example.com", result.email)
        }

        @Test
        fun `should throw on user not found`() {
            val dto = createUserUpdateDto()

            every { serviceSpy.getById(dto.id, any(), any()) } throws NotFoundException(defaultExceptionMessages.userNotFound())

            assertThrows<NotFoundException> {
                serviceSpy.update(dto)
            }

            verify { serviceSpy.getById(dto.id, any(), any()) }
            verify(exactly = 0) { repository.save(any()) }
        }
    }

    @Nested
    inner class DeleteByIdTests {
        @Test
        fun `should delete user by ID`() {
            val user = createUser()

            every { serviceSpy.getById(any(), any(), any()) } returns user
            every { repository.deleteById(any()) } returns Unit

            serviceSpy.deleteById(user.id)

            verify { serviceSpy.getById(user.id, any(), any()) }
            verify { repository.deleteById(user.id) }
        }

        @Test
        fun `should throw exception when user not found`() {
            every { serviceSpy.getById(any(), any(), any()) } throws NotFoundException(defaultExceptionMessages.userNotFound())

            assertThrows<NotFoundException> {
                serviceSpy.deleteById(1)
            }

            verify { serviceSpy.getById(1, any(), any()) }
            verify(exactly = 0) { repository.deleteById(any()) }
        }
    }

    @Nested
    inner class DeleteAllTests {

        @Test
        fun `should delete all users and return String`() {
            every { repository.deleteAll() } returns mockk()

            val result = service.deleteAll()

            assertEquals(result, "Все пользователи удалены успешно.")

            verify { repository.deleteAll() }
        }
    }
}