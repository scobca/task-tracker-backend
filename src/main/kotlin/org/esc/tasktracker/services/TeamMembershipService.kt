package org.esc.tasktracker.services

import org.esc.tasktracker.dto.filters.TeamMembershipFilterDto
import org.esc.tasktracker.dto.teams.CreateTeamMembershipDto
import org.esc.tasktracker.dto.teams.UpdateTeamMembershipDto
import org.esc.tasktracker.entities.TeamMembership
import org.esc.tasktracker.enums.DefaultExceptionMessages
import org.esc.tasktracker.enums.DefaultExceptionMessages.Companion.getMessage
import org.esc.tasktracker.exceptions.DoubleRecordException
import org.esc.tasktracker.exceptions.NotFoundException
import org.esc.tasktracker.interfaces.CrudService
import org.esc.tasktracker.mappers.TeamsMapper
import org.esc.tasktracker.repositories.TeamMembershipRepository
import org.esc.tasktracker.repositories.specs.TeamMembershipSpecifications
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TeamMembershipService(
    override val repository: TeamMembershipRepository,
    private val teamsService: TeamsService,
    private val usersService: UsersService,
    private val teamsMapper: TeamsMapper,
    private val specifications: TeamMembershipSpecifications
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
            DefaultExceptionMessages.TEAM_MEMBERSHIP_NOT_FOUND
        )
    }

    @EventListener
    @Transactional
    override fun create(item: CreateTeamMembershipDto): TeamMembership {
        repository.findByUserIdAndTeamId(item.userId, item.teamId)?.let {
            throw DoubleRecordException(
                DefaultExceptionMessages.TEAM_MEMBERSHIP_DOUBLE_RECORD
            )
        }

        val user = usersService.getById(item.userId, message = DefaultExceptionMessages.USER_NOT_FOUND.getMessage())!!
        val team = teamsService.getById(item.teamId, message = DefaultExceptionMessages.TEAM_NOT_FOUND.getMessage())!!

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
            message = DefaultExceptionMessages.TEAM_MEMBERSHIP_NOT_FOUND.getMessage()
        )!!
            .let {
                repository.delete(it)
                "Участник команды удален."
            }
    }

    @Transactional
    fun deleteById(userId: Long, teamId: Long): String {
        return getById(userId, teamId).let { record ->
            repository.delete(record)
            "Участник команды удален."
        }
    }

    @Transactional
    fun deleteAllMembers(teamId: Long): String {
        return teamsService.getById(teamId, message = DefaultExceptionMessages.TEAM_NOT_FOUND.getMessage())!!
            .let {
                repository.deleteAllByTeam(it)
                "Все участники команды удалены."
            }
    }

    @Transactional
    override fun deleteAll(): String {
        repository.deleteAll()
        return "Все записи об участиях в командах удалены."
    }
}