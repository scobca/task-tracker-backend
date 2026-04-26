package org.esc.tasktracker.mappers

import org.esc.tasktracker.dto.jwt.SaveRefreshTokenDto
import org.esc.tasktracker.entities.JwtTokensStorage
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.stereotype.Component

@Component
class JwtTokensStorageMapper {

    fun tokenFromSaveRefreshDto(o: SaveRefreshTokenDto): JwtTokensStorage {
        return JwtTokensStorage(
            id = 0L,
            user = o.user,
            uuid = o.uuid,
            token = o.token
        )
    }
}