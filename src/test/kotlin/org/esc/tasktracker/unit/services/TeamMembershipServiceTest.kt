package org.esc.tasktracker.unit.services

import io.mockk.*
import org.esc.tasktracker.dto.filters.TeamMembershipFilterDto
import org.esc.tasktracker.entities.TeamMembership
import org.esc.tasktracker.exceptions.DefaultExceptionMessages
import org.esc.tasktracker.enums.TeamRoles
import org.esc.tasktracker.exceptions.DoubleRecordException
import org.esc.tasktracker.exceptions.NotFoundException
import org.esc.tasktracker.mappers.TeamsMapper
import org.esc.tasktracker.repositories.TeamMembershipRepository
import org.esc.tasktracker.repositories.specs.TeamMembershipSpecifications
import org.esc.tasktracker.services.TeamMembershipService
import org.esc.tasktracker.services.TeamsService
import org.esc.tasktracker.services.UsersService
import org.esc.tasktracker.unit.generators.*
import org.esc.tasktracker.utils.MessageResolver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TeamMembershipServiceTest {
    private val repository = mockk<TeamMembershipRepository>()
    private val teamsService = mockk<TeamsService>()
    private val usersService = mockk<UsersService>()
    private val teamsMapper = mockk<TeamsMapper>()
    private val specifications = mockk<TeamMembershipSpecifications>()
    private val messageResolver = mockk<MessageResolver>()
    private var defaultExceptionMessages = mockk<DefaultExceptionMessages>()
    private val service = TeamMembershipService(
        repository,
        teamsService,
        usersService,
        teamsMapper,
        specifications,
        messageResolver,
        defaultExceptionMessages
    )
    private lateinit var serviceSpy: TeamMembershipService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        serviceSpy = spyk(
            TeamMembershipService(
                repository,
                teamsService,
                usersService,
                teamsMapper,
                specifications,
                messageResolver,
                defaultExceptionMessages
            )
        )

        every { defaultExceptionMessages.teamMembershipNotFound() } returns "No record of participation with this set of parameters was found."
        every { defaultExceptionMessages.teamMembershipDoubleRecord() } returns "This user is already a member of this team."
        every { defaultExceptionMessages.teamNotFound() } returns "A team with this ID was not found."
        every { defaultExceptionMessages.userNotFound() } returns "The user with this ID was not found."

        every { messageResolver.getMessage("response.teamMembership.deleteById") } returns "Участник команды удален успешно."
        every { messageResolver.getMessage("response.teamMembership.deleteAllByTeam") } returns "Все участники команды удалены."
        every { messageResolver.getMessage("response.teamMembership.deleteAll") } returns "Все записи об участиях в командах удалены."
    }

    @Nested
    inner class GetAllTests {

        val teamMember1 = createTeamMembership()
        val teamMember2 = createTeamMembership()

        @Test
        fun `getAll with no filters returns all teams`() {
            every { specifications.hasUserId(any<Long>()) } returns null
            every { specifications.hasTeamId(any<Long>()) } returns null
            every { specifications.hasTeamRole(any<TeamRoles>()) } returns null

            every { repository.findAll(any<Specification<TeamMembership>>(), any<Pageable>()) } returns PageImpl(
                listOf(teamMember1, teamMember2)
            )

            val result = service.getAll(null, Pageable.unpaged())

            assertEquals(2, result.content.size)
            assertEquals(1, result.totalPages)
            assertTrue { result.isLast }

            assertEquals(teamMember1, result.content[0])
            assertEquals(teamMember2, result.content[1])
        }

        @Test
        fun `getAll with filter - verifies specification call`() {
            every { specifications.hasUserId(1) } returns mockk()
            every { specifications.hasTeamId(null) } returns null
            every { specifications.hasTeamRole(null) } returns null

            every { repository.findAll(any<Specification<TeamMembership>>(), any<Pageable>()) } returns PageImpl(
                emptyList()
            )

            service.getAll(TeamMembershipFilterDto(userId = 1, teamId = null, teamRole = null), Pageable.unpaged())

            verify { specifications.hasUserId(1) }
        }
    }

    @Nested
    inner class GetByIdTests {

        @Test
        fun `should return team membership by ID`() {
            val teamMembership = createTeamMembership(id = 1)

            every { repository.findById(any()) } returns Optional.of(teamMembership)

            val result = service.getById(
                teamMembership.id,
                throwable = true,
                message = defaultExceptionMessages.teamMembershipNotFound()
            )

            assertEquals(teamMembership, result)
            verify { repository.findById(any()) }
        }

        @Test
        fun `should throw exception when team membership not found & throwable is true`() {
            every { repository.findById(any()) } returns Optional.empty()
            val exception = assertFailsWith<NotFoundException> {
                service.getById(
                    1,
                    throwable = true,
                    message = defaultExceptionMessages.teamMembershipNotFound()
                )
            }

            verify { repository.findById(any()) }
            assertEquals(exception.message, defaultExceptionMessages.teamMembershipNotFound())
        }

        @Test
        fun `should return null when team membership not found & throwable is false`() {
            every { repository.findById(any()) } returns Optional.empty()

            val result = service.getById(
                1,
                throwable = false,
                message = defaultExceptionMessages.teamMembershipNotFound()
            )

            verify { repository.findById(any()) }
            assertEquals(null, result)
        }
    }

    @Nested
    inner class GetByUserIdAndTeamIdTests {

        @Test
        fun `should return team membership by UserID and TeamID`() {
            val teamMembership = createTeamMembership(user = createUser(id = 1), team = createTeam(id = 1))

            every { repository.findByUserIdAndTeamId(any(), any()) } returns teamMembership

            val result = service.getById(userId = 1, teamId = 1)

            assertEquals(teamMembership, result)
            verify { repository.findByUserIdAndTeamId(any(), any()) }
        }

        @Test
        fun `should throw exception when team membership not found`() {
            every { repository.findByUserIdAndTeamId(any(), any()) } returns null

            val exception = assertFailsWith<NotFoundException> { service.getById(userId = 1, teamId = 1) }

            verify { repository.findByUserIdAndTeamId(any(), any()) }
            assertEquals(exception.message, defaultExceptionMessages.teamMembershipNotFound())
        }
    }

    @Nested
    inner class CreateTests {

        @Test
        fun `should create new team membership`() {
            val user = createUser(id = 1)
            val team = createTeam(id = 1)
            val dto = createTeamMembershipCreateDto(userId = user.id, teamId = team.id)
            val teamMembership = createTeamMembership()

            every { repository.findByUserIdAndTeamId(any(), any()) } returns null
            every {
                usersService.getById(
                    any(),
                    throwable = any(),
                    message = any()
                )
            } returns user
            every {
                teamsService.getById(
                    any(),
                    throwable = any(),
                    message = any()
                )
            } returns team
            every { teamsMapper.teamMembershipFromDto(any(), any(), any()) } returns teamMembership
            every { repository.save(any()) } returns teamMembership

            val result = service.create(dto)

            verify {
                repository.findByUserIdAndTeamId(any(), any())
                usersService.getById(any(), any(), any())
                teamsService.getById(any(), any(), any())
                teamsMapper.teamMembershipFromDto(any(), any(), any())
                repository.save(any())
            }
            assertEquals(teamMembership, result)
        }

        @Test
        fun `should throw DoubleRecord exception when similar combination of userID and teamID already saved`() {
            val dto = createTeamMembershipCreateDto()
            val teamMembership = createTeamMembership()

            every { repository.findByUserIdAndTeamId(any(), any()) } returns teamMembership

            val exception = assertFailsWith<DoubleRecordException> { service.create(dto) }

            verify { repository.findByUserIdAndTeamId(any(), any()) }
            verify(exactly = 0) {
                usersService.getById(any(), any(), any())
                teamsService.getById(any(), any(), any())
                teamsMapper.teamMembershipFromDto(any(), any(), any())
                repository.save(any())
            }
            assertEquals(exception.message, defaultExceptionMessages.teamMembershipDoubleRecord())
        }

        @Test
        fun `should throw NotFoundException when user not found`() {
            val dto = createTeamMembershipCreateDto()

            every { repository.findByUserIdAndTeamId(any(), any()) } returns null
            every {
                usersService.getById(
                    any(),
                    any(),
                    any()
                )
            } throws NotFoundException(defaultExceptionMessages.userNotFound())

            val exception = assertFailsWith<NotFoundException> { service.create(dto) }

            verify {
                repository.findByUserIdAndTeamId(any(), any())
                usersService.getById(any(), any(), any())
            }
            verify(exactly = 0) {
                teamsService.getById(any(), any(), any())
                teamsMapper.teamMembershipFromDto(any(), any(), any())
                repository.save(any())
            }
            assertEquals(exception.message, defaultExceptionMessages.userNotFound())
        }

        @Test
        fun `should throw NotFoundException when team not found`() {
            val user = createUser()
            val dto = createTeamMembershipCreateDto()

            every { repository.findByUserIdAndTeamId(any(), any()) } returns null
            every { usersService.getById(any(), any(), any()) } returns user
            every {
                teamsService.getById(
                    any(),
                    any(),
                    any()
                )
            } throws NotFoundException(defaultExceptionMessages.teamNotFound())

            val exception = assertFailsWith<NotFoundException> { service.create(dto) }

            verify {
                repository.findByUserIdAndTeamId(any(), any())
                usersService.getById(any(), any(), any())
                teamsService.getById(any(), any(), any())
            }
            verify(exactly = 0) {
                teamsMapper.teamMembershipFromDto(any(), any(), any())
                repository.save(any())
            }
            assertEquals(exception.message, defaultExceptionMessages.teamNotFound())
        }
    }

    @Nested
    inner class UpdateTests {

        @Test
        fun `should update team membership role`() {
            val user = createUser(id = 1)
            val team = createTeam(id = 1)
            val dto = createTeamMembershipUpdateDto(userId = user.id, teamId = team.id, role = TeamRoles.ADMIN)
            val teamMembershipOld = createTeamMembership(role = TeamRoles.MEMBER)
            val teamMembershipNew = createTeamMembership(role = TeamRoles.ADMIN)


            every { serviceSpy.getById(any<Long>(), any<Long>()) } returns teamMembershipOld
            every { repository.save(any()) } returns teamMembershipNew

            val result = serviceSpy.update(dto)

            verify {
                serviceSpy.getById(any<Long>(), any<Long>())
                repository.save(any())
            }
            assertEquals(teamMembershipNew, result)
        }

        @Test
        fun `should throw NotFoundException when team membership not found`() {
            val dto = createTeamMembershipUpdateDto()

            every {
                serviceSpy.getById(
                    any<Long>(),
                    any<Long>()
                )
            } throws NotFoundException(defaultExceptionMessages.teamMembershipNotFound())

            val exception = assertFailsWith<NotFoundException> { serviceSpy.update(dto) }

            verify { serviceSpy.getById(any<Long>(), any<Long>()) }
            verify(exactly = 0) { repository.save(any()) }
            assertEquals(exception.message, defaultExceptionMessages.teamMembershipNotFound())
        }
    }

    @Nested
    inner class DeleteByIdTests {

        @Test
        fun `should delete team membership`() {
            val teamMembership = createTeamMembership(id = 1)

            every { serviceSpy.getById(any<Long>(), any<Boolean>(), any()) } returns teamMembership
            every { repository.delete(any<TeamMembership>()) } just runs

            val result = serviceSpy.deleteById(1)

            verify {
                serviceSpy.getById(any<Long>(), any<Boolean>(), any())
                repository.delete(any<TeamMembership>())
            }
            assertEquals(result, messageResolver.getMessage("response.teamMembership.deleteById"))
        }

        @Test
        fun `should throw NotFoundException when team membership not found`() {
            every { serviceSpy.getById(any<Long>(), any<Boolean>(), any()) } throws NotFoundException(
                defaultExceptionMessages.teamMembershipNotFound()
            )

            val exception = assertFailsWith<NotFoundException> { serviceSpy.deleteById(1) }

            verify { serviceSpy.getById(any<Long>(), any<Boolean>(), any()) }
            verify(exactly = 0) { repository.delete(any<TeamMembership>()) }
            assertEquals(exception.message, defaultExceptionMessages.teamMembershipNotFound())
        }
    }

    @Nested
    inner class DeleteByUserIdAndTeamIdTests {

        @Test
        fun `should delete team membership`() {
            val teamMembership = createTeamMembership()

            every { serviceSpy.getById(any<Long>(), any<Long>()) } returns teamMembership
            every { repository.delete(any<TeamMembership>()) } just runs

            val result = serviceSpy.deleteById(1, 1)

            verify {
                serviceSpy.getById(any<Long>(), any<Long>())
                repository.delete(any<TeamMembership>())
            }
            assertEquals(result, messageResolver.getMessage("response.teamMembership.deleteById"))
        }

        @Test
        fun `should throw NotFoundException when team membership not found`() {
            every {
                serviceSpy.getById(
                    any<Long>(),
                    any<Long>()
                )
            } throws NotFoundException(defaultExceptionMessages.teamMembershipNotFound())

            val exception = assertFailsWith<NotFoundException> { serviceSpy.deleteById(1, 1) }

            verify { serviceSpy.getById(any<Long>(), any<Long>()) }
            verify(exactly = 0) { repository.delete(any<TeamMembership>()) }
            assertEquals(exception.message, defaultExceptionMessages.teamMembershipNotFound())
        }
    }

    @Nested
    inner class DeleteAllMembersTests {

        @Test
        fun `should delete all team's memberships`() {
            val team = createTeam(id = 1)

            every { teamsService.getById(any<Long>(), any(), any()) } returns team
            every { repository.deleteAllByTeam(any()) } just runs

            val result = serviceSpy.deleteAllMembers(1)

            verify {
                teamsService.getById(any<Long>(), any(), any())
                repository.deleteAllByTeam(any())
            }
            assertEquals(result, messageResolver.getMessage("response.teamMembership.deleteAllByTeam"))
        }

        @Test
        fun `should return NotFoundException when team not found`() {
            every {
                teamsService.getById(
                    any<Long>(),
                    any(),
                    any()
                )
            } throws NotFoundException(defaultExceptionMessages.teamNotFound())

            val exception = assertFailsWith<NotFoundException> { serviceSpy.deleteAllMembers(1) }

            verify { teamsService.getById(any<Long>(), any(), any()) }
            verify(exactly = 0) { repository.deleteAllByTeam(any()) }
            assertEquals(exception.message, defaultExceptionMessages.teamNotFound())

        }
    }

    @Nested
    inner class DeleteAllTests {

        @Test
        fun `should delete all team memberships`() {
            every { repository.deleteAll() } just runs

            val result = service.deleteAll()

            verify { repository.deleteAll() }
            assertEquals(result, messageResolver.getMessage("response.teamMembership.deleteAll"))
        }
    }

}