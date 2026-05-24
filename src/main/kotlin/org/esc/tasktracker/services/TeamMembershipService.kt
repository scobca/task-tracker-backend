package org.esc.tasktracker.services

import org.esc.tasktracker.dto.filters.TeamMembershipFilterDto
import org.esc.tasktracker.dto.teams.CreateTeamMembershipDto
import org.esc.tasktracker.dto.teams.UpdateTeamMembershipDto
import org.esc.tasktracker.entities.TeamMembership
import org.esc.tasktracker.exceptions.DefaultExceptionMessages
import org.esc.tasktracker.exceptions.DoubleRecordException
import org.esc.tasktracker.exceptions.NotFoundException
import org.esc.tasktracker.interfaces.CrudService
import org.esc.tasktracker.mappers.TeamsMapper
import org.esc.tasktracker.repositories.TeamMembershipRepository
import org.esc.tasktracker.repositories.specs.TeamMembershipSpecifications
import org.esc.tasktracker.utils.MessageResolver
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TeamMembershipService(
    override val repository: TeamMembershipRepository,
    private val teamsService: TeamsService,
    private val usersService: UsersService,
    private val teamsMapper: TeamsMapper,
    private val specifications: TeamMembershipSpecifications,
    private val messageResolver: MessageResolver,
    private val defaultExceptionMessages: DefaultExceptionMessages,
) : CrudService<TeamMembership, Long, CreateTeamMembershipDto, UpdateTeamMembershipDto, TeamMembershipFilterDto> {

    override fun getAll(filters: TeamMembershipFilterDto?, pageable: Pageable): Page<TeamMembership> {
        val specs = listOfNotNull(
            specifications.hasUserId(filters?.userId),
            specifications.hasTeamId(filters?.teamId),
            specifications.hasTeamRole(filters?.teamRole)
        )

        return repository.findAll(Specification.allOf(specs), pageable)
    }

    fun getById(userId: Long, teamId: Long): TeamMembership {
        return repository.findByUserIdAndTeamId(userId, teamId) ?: throw NotFoundException(
            defaultExceptionMessages.teamMembershipNotFound()
        )
    }

    @EventListener
    @Transactional
    override fun create(item: CreateTeamMembershipDto): TeamMembership {
        repository.findByUserIdAndTeamId(item.userId, item.teamId)?.let {
            throw DoubleRecordException(
                defaultExceptionMessages.teamMembershipDoubleRecord()
            )
        }

        val user = usersService.getById(item.userId, message = defaultExceptionMessages.userNotFound())!!
        val team = teamsService.getById(item.teamId, message = defaultExceptionMessages.teamNotFound())!!

        return repository.save(teamsMapper.teamMembershipFromDto(user, team, item.role))
    }

    @Transactional
    override fun update(item: UpdateTeamMembershipDto): TeamMembership {
        return getById(item.userId, item.teamId)
            .let { record ->
                item.role?.let { record.role = it }
                repository.save(record)
            }
    }

    @Transactional
    override fun deleteById(id: Long): String {
        return getById(
            id,
            throwable = true,
            message = defaultExceptionMessages.teamMembershipNotFound()
        )!!
            .let {
                repository.delete(it)
                messageResolver.getMessage("response.teamMembership.deleteById")
            }
    }

    @Transactional
    fun deleteById(userId: Long, teamId: Long): String {
        return getById(userId, teamId).let { record ->
            repository.delete(record)
            messageResolver.getMessage("response.teamMembership.deleteById")
        }
    }

    @Transactional
    fun deleteAllMembers(teamId: Long): String {
        return teamsService.getById(teamId, message = defaultExceptionMessages.teamNotFound())!!
            .let {
                repository.deleteAllByTeam(it)
                messageResolver.getMessage("response.teamMembership.deleteAllByTeam")
            }
    }

    @Transactional
    override fun deleteAll(): String {
        repository.deleteAll()
        return messageResolver.getMessage("response.teamMembership.deleteAll")
    }
}