package org.esc.tasktracker.unit.services

import io.mockk.*
import org.esc.tasktracker.dto.filters.TeamsFilterDto
import org.esc.tasktracker.dto.teams.CreateTeamMembershipDto
import org.esc.tasktracker.entities.Teams
import org.esc.tasktracker.exceptions.DefaultExceptionMessages
import org.esc.tasktracker.exceptions.NotFoundException
import org.esc.tasktracker.mappers.TeamsMapper
import org.esc.tasktracker.repositories.TeamsRepository
import org.esc.tasktracker.repositories.specs.TeamsSpecifications
import org.esc.tasktracker.services.TeamsService
import org.esc.tasktracker.services.UsersService
import org.esc.tasktracker.unit.generators.createTeam
import org.esc.tasktracker.unit.generators.createTeamCreateDto
import org.esc.tasktracker.unit.generators.createTeamUpdateDto
import org.esc.tasktracker.unit.generators.createUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TeamsServiceTest {
    private val repository = mockk<TeamsRepository>()
    private val teamsMapper = mockk<TeamsMapper>()
    private val teamsSpecifications = mockk<TeamsSpecifications>()
    private val usersService = mockk<UsersService>()
    private val applicationEventPublisher = mockk<ApplicationEventPublisher>()
    private var defaultExceptionMessages = mockk<DefaultExceptionMessages>()
    private val service = TeamsService(
        repository,
        teamsMapper,
        teamsSpecifications,
        usersService,
        applicationEventPublisher,
        defaultExceptionMessages
    )

    private lateinit var serviceSpy: TeamsService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        serviceSpy = spyk(
            TeamsService(
                repository,
                teamsMapper,
                teamsSpecifications,
                usersService,
                applicationEventPublisher,
                defaultExceptionMessages
            )
        )

        every { defaultExceptionMessages.teamNotFound() } returns "A team with this ID was not found."
        every { defaultExceptionMessages.userNotFound() } returns "The user with this ID was not found."
    }

    @Nested
    inner class GetAllTests {

        val team1 = createTeam()
        val team2 = createTeam()

        @Test
        fun `getAll with no filters returns all teams`() {
            every { teamsSpecifications.hasName(any()) } returns null
            every { teamsSpecifications.hasOwnerId(any<Long>()) } returns null
            every { repository.findAll(any<Specification<Teams>>(), any<Pageable>()) } returns PageImpl(
                listOf(team1, team2)
            )

            val result = service.getAll(null, Pageable.unpaged())

            assertEquals(2, result.content.size)
            assertEquals(1, result.totalPages)
            assertTrue { result.isLast }

            assertEquals(team1, result.content[0])
            assertEquals(team2, result.content[1])
        }

        @Test
        fun `getAll with name filter - verifies specification call`() {
            every { teamsSpecifications.hasName("Тест 1") } returns mockk()
            every { teamsSpecifications.hasOwnerId(null) } returns null
            every { repository.findAll(any<Specification<Teams>>(), any<Pageable>()) } returns PageImpl(emptyList())

            service.getAll(TeamsFilterDto(name = "Тест 1", ownerId = null), Pageable.unpaged())

            verify { teamsSpecifications.hasName("Тест 1") }
        }

        @Test
        fun `getAll with ownerID filter - verifies specification call`() {
            every { teamsSpecifications.hasName(null) } returns null
            every { teamsSpecifications.hasOwnerId(1) } returns mockk()
            every { repository.findAll(any<Specification<Teams>>(), any<Pageable>()) } returns PageImpl(emptyList())

            service.getAll(TeamsFilterDto(name = null, ownerId = 1), Pageable.unpaged())

            verify { teamsSpecifications.hasOwnerId(1) }
        }
    }

    @Nested
    inner class GetByIdTests {

        @Test
        fun `should return team by id`() {
            val team1 = createTeam()

            every { repository.findById(any<Long>()) } returns Optional.of(team1)

            val result = service.getById(team1.id, true)

            assertEquals(team1, result)
            verify { repository.findById(any()) }
        }

        @Test
        fun `should throw exception when team not found & throwable = true`() {
            every { repository.findById(any<Long>()) } returns Optional.empty()

            val exception = assertFailsWith<NotFoundException> {
                service.getById(id = 1, throwable = true, message = defaultExceptionMessages.teamNotFound())
            }

            verify { repository.findById(any()) }
            assertEquals(exception.message, defaultExceptionMessages.teamNotFound())
        }

        @Test
        fun `should return null when team not found & throwable = false`() {
            every { repository.findById(any<Long>()) } returns Optional.empty()

            val result = service.getById(id = 1, throwable = false, message = defaultExceptionMessages.teamNotFound())

            verify { repository.findById(any()) }
            assertEquals(result, null)
        }
    }

    @Nested
    inner class GetByOwnerIdTests {

        @Test
        fun `should return list of teams by userID`() {
            val user = createUser()
            val teams = List(5) { createTeam(user = user) }

            every { usersService.getById(any(), any(), any()) } returns user
            every { repository.findByOwner(any(), any()) } returns PageImpl(teams)

            val result = service.getByUserId(user.id, pageable = Pageable.unpaged())

            assertEquals(teams, result.content)

            verify {
                usersService.getById(any(), any(), any())
                repository.findByOwner(user, Pageable.unpaged())
            }
        }

        @Test
        fun `should throw exception when user not found`() {
            val user = createUser()

            every { usersService.getById(any(), any(), any()) } throws NotFoundException(defaultExceptionMessages.userNotFound())

            val exception =
                assertFailsWith<NotFoundException> { service.getByUserId(user.id, pageable = Pageable.unpaged()) }

            assertEquals(exception.message, defaultExceptionMessages.userNotFound())
            verify { usersService.getById(any(), any(), any()) }
            verify(exactly = 0) { repository.findByOwner(any(), any()) }
        }
    }

    @Nested
    inner class CreateTests {

        @Test
        fun `should create team`() {
            val dto = createTeamCreateDto()
            val user = createUser()
            val team = createTeam()

            every { applicationEventPublisher.publishEvent(any<CreateTeamMembershipDto>()) } just runs
            every { usersService.getById(any(), throwable = true, message = any()) } returns user
            every { teamsMapper.teamFromDto(any(), any()) } returns team
            every { repository.save(any()) } returns team

            val result = service.create(dto)

            verify {
                usersService.getById(any(), any(), any())
                teamsMapper.teamFromDto(dto, user)
                repository.save(team)
            }

            assertEquals(team, result)
        }

        @Test
        fun `should throw exception when user doesn't exists`() {
            val dto = createTeamCreateDto()

            every {
                usersService.getById(
                    any(),
                    throwable = true,
                    message = any()
                )
            } throws NotFoundException(defaultExceptionMessages.userNotFound())

            val exception = assertFailsWith<NotFoundException> {
                service.create(dto)
            }

            verify { usersService.getById(any(), any(), any()) }
            verify(exactly = 0) {
                teamsMapper.teamFromDto(any(), any())
                repository.save(any())
            }

            assertEquals(exception.message, defaultExceptionMessages.userNotFound())
        }
    }

    @Nested
    inner class UpdateTests {
        @Test
        fun `should update only name`() {
            val team = createTeam(id = 1L, name = "Old Name")
            val dto = createTeamUpdateDto(id = 1L, name = "New Name")

            every { serviceSpy.getById(dto.id, any(), any()) } returns team
            every { repository.save(team) } returns team.copy(name = "New Name")

            val result = serviceSpy.update(dto)

            verify { serviceSpy.getById(dto.id, any(), any()) }
            verify { repository.save(team) }

            assertEquals("New Name", result.name)
        }

        @Test
        fun `should update only description`() {
            val team = createTeam(id = 1L, description = "Old desc", name = "Old Name")
            val dto = createTeamUpdateDto(id = 1L, description = "New desc")

            every { serviceSpy.getById(dto.id, any(), any()) } returns team
            every { repository.save(team) } returns team.copy(description = "New desc")

            val result = serviceSpy.update(dto)

            verify { serviceSpy.getById(dto.id, any(), any()) }
            verify { repository.save(team) }

            assertEquals("New desc", result.description)
            assertEquals("Old Name", result.name)
        }

        @Test
        fun `should update only owner (user exists)`() {
            val team = createTeam(id = 1L)
            val newOwner = createUser(id = 10L)
            val dto = createTeamUpdateDto(id = 1L, userId = 10L)

            every { serviceSpy.getById(dto.id, any(), any()) } returns team
            every { usersService.getById(10L, true, any()) } returns newOwner
            every { repository.save(team) } returns team.copy(owner = newOwner)

            val result = serviceSpy.update(dto)

            verify { serviceSpy.getById(dto.id, any(), any()) }
            verify { usersService.getById(10L, true, any()) }
            verify { repository.save(team) }

            assertEquals(newOwner.id, result.owner.id)
        }

        @Test
        fun `should update name, description and owner`() {
            val team = createTeam(id = 1L, name = "Old", description = "Old desc")
            val newOwner = createUser(id = 10L)
            val dto = createTeamUpdateDto(
                id = 1L,
                name = "New Name",
                description = "New desc",
                userId = 10L
            )

            every { serviceSpy.getById(dto.id, any(), any()) } returns team
            every { usersService.getById(10L, true, any()) } returns newOwner
            every { repository.save(any()) } returns team.copy(
                name = "New Name",
                description = "New desc",
                owner = newOwner
            )

            val result = serviceSpy.update(dto)

            verify {
                serviceSpy.getById(dto.id, any(), any())
                usersService.getById(10L, true, any())
                repository.save(team)
            }

            assertEquals("New Name", result.name)
            assertEquals("New desc", result.description)
            assertEquals(newOwner.id, result.owner.id)
        }

        @Test
        fun `should trim name on update`() {
            val team = createTeam(id = 1L, name = " Old ")
            val dto = createTeamUpdateDto(id = 1L, name = " New Name ")

            every { serviceSpy.getById(dto.id, any(), any()) } returns team
            every { repository.save(team) } returns team.copy(name = "New Name")

            serviceSpy.update(dto)

            verify { repository.save(any()) }
        }

        @Test
        fun `should throw on team not found`() {
            val dto = createTeamUpdateDto(id = 1L)

            every { serviceSpy.getById(dto.id, any(), any()) } throws NotFoundException(defaultExceptionMessages.teamNotFound())

            assertFailsWith<NotFoundException> {
                serviceSpy.update(dto)
            }

            verify { serviceSpy.getById(dto.id, any(), any()) }
            verify(exactly = 0) { repository.save(any()) }
            verify(exactly = 0) { usersService.getById(any(), any(), any()) }
        }

        @Test
        fun `should throw on user not found`() {
            val team = createTeam(id = 1L)
            val dto = createTeamUpdateDto(id = 1L, userId = 999L)

            every { serviceSpy.getById(dto.id, any(), any()) } returns team
            every {
                usersService.getById(
                    999L,
                    true,
                    any()
                )
            } throws NotFoundException(defaultExceptionMessages.userNotFound())

            assertFailsWith<NotFoundException> {
                serviceSpy.update(dto)
            }

            verify { serviceSpy.getById(dto.id, any(), any()) }
            verify { usersService.getById(999L, true, any()) }
            verify(exactly = 0) { repository.save(any()) }
        }
    }

    @Nested
    inner class DeleteByIdTests {
        @Test
        fun `should delete user by ID`() {
            val team = createTeam()

            every { serviceSpy.getById(any(), any(), any()) } returns team
            every { repository.deleteById(any()) } returns Unit

            serviceSpy.deleteById(team.id)

            verify { serviceSpy.getById(team.id, any(), any()) }
            verify { repository.deleteById(team.id) }
        }

        @Test
        fun `should throw exception when user not found`() {
            every { serviceSpy.getById(any(), any(), any()) } throws NotFoundException(defaultExceptionMessages.teamNotFound())

            assertFailsWith<NotFoundException> {
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

            assertEquals(result, "Все команды удалены успешно.")

            verify { repository.deleteAll() }
        }
    }

}