package org.esc.tasktracker.mappers

import org.esc.tasktracker.dto.users.CreateUserDto
import org.esc.tasktracker.entities.Users
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface UsersMapper {

    @Mapping(target = "id", ignore = true)
    fun userFromDto(dto: CreateUserDto): Users
}