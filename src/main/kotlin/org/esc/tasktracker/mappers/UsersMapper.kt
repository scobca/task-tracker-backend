package org.esc.tasktracker.mappers

import org.esc.tasktracker.dto.users.CreateUserDto
import org.esc.tasktracker.entities.Users
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.stereotype.Component

@Component
class UsersMapper {

    fun userFromDto(dto: CreateUserDto): Users {
        return Users(
            id = 0L,
            name = dto.name,
            email = dto.email,
            password = dto.password,
            createdAt = null,
            updatedAt = null
        )
    }
}