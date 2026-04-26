package org.esc.tasktracker.mappers

import org.esc.tasktracker.dto.teams.CreateTeamDto
import org.esc.tasktracker.entities.TeamMembership
import org.esc.tasktracker.entities.Teams
import org.esc.tasktracker.entities.Users
import org.esc.tasktracker.enums.TeamRoles
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.springframework.stereotype.Component

@Component
class TeamsMapper {

    fun teamFromDto(dto: CreateTeamDto, user: Users): Teams {
        return Teams(
            id = 0L,
            name = dto.name,
            description = dto.description,
            owner = user,
            createdAt = null,
            updatedAt = null
        )
    }

    fun teamMembershipFromDto(user: Users, team: Teams, role: TeamRoles): TeamMembership {
        return TeamMembership(
            id = 0L,
            user = user,
            team = team,
            role = role,
            createdAt = null,
            updatedAt = null
        )
    }
}
