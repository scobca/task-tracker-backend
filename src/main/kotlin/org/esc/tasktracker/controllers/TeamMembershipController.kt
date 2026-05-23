package org.esc.tasktracker.controllers

import org.esc.tasktracker.dto.filters.TeamMembershipFilterDto
import org.esc.tasktracker.dto.teams.CreateTeamMembershipDto
import org.esc.tasktracker.dto.teams.UpdateTeamMembershipDto
import org.esc.tasktracker.entities.TeamMembership
import org.esc.tasktracker.exceptions.DefaultExceptionMessages
import org.esc.tasktracker.extensions.toHttpResponse
import org.esc.tasktracker.interfaces.CrudController
import org.esc.tasktracker.services.TeamMembershipService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.ProjectedPayload
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/teams/members")
class TeamMembershipController(
    private val service: TeamMembershipService,
    private val defaultExceptionMessages: DefaultExceptionMessages,
) :
    CrudController<TeamMembership, Long, CreateTeamMembershipDto, UpdateTeamMembershipDto, TeamMembershipFilterDto> {

    @GetMapping
    override fun getAll(
        @ModelAttribute filters: TeamMembershipFilterDto,
        @ProjectedPayload
        @PageableDefault(size = 20, sort = ["id"])
        pageable: Pageable,
    ) = service.getAll(filters, pageable)

    @GetMapping("/{id}")
    override fun getById(@PathVariable id: Long) =
        service.getById(
            id,
            throwable = true,
            message = defaultExceptionMessages.teamMembershipNotFound()
        )!!

    @GetMapping("/by-user/{userId}/by-team/{teamId}")
    fun getByUserAndTeam(
        @PathVariable userId: Long,
        @PathVariable teamId: Long
    ) = service.getById(userId, teamId)

    @PostMapping
    override fun create(@RequestBody item: CreateTeamMembershipDto) = service.create(item)

    @PatchMapping
    override fun update(@RequestBody item: UpdateTeamMembershipDto) = service.update(item)

    @DeleteMapping("/{id}")
    override fun deleteById(@PathVariable id: Long) = service.deleteById(id).toHttpResponse()

    @DeleteMapping("/by-user/{userId}/by-team/{teamId}")
    fun deleteByUserAndTeam(@PathVariable userId: Long, @PathVariable teamId: Long) =
        service.deleteById(userId, teamId).toHttpResponse()

    @DeleteMapping("/deleteAllMembers/{teamId}")
    fun deleteAllMembers(@PathVariable teamId: Long) = service.deleteAllMembers(teamId).toHttpResponse()

    @DeleteMapping
    override fun deleteAll() = service.deleteAll().toHttpResponse()
}