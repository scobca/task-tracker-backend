package org.esc.tasktracker.mappers

import org.esc.tasktracker.dto.jwt.SaveRefreshTokenDto
import org.esc.tasktracker.entities.JwtTokensStorage
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface JwtTokensStorageMapper {

    @Mapping(target = "id", ignore = true)
    fun tokenFromSaveRefreshDto(o: SaveRefreshTokenDto): JwtTokensStorage
}