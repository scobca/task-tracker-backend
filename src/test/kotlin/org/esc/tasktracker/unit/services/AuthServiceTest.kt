package org.esc.tasktracker.unit.services

import io.mockk.*
import org.esc.tasktracker.exceptions.DefaultExceptionMessages
import org.esc.tasktracker.exceptions.DoubleRecordException
import org.esc.tasktracker.exceptions.JwtAuthenticationException
import org.esc.tasktracker.security.JwtUtil
import org.esc.tasktracker.services.AuthService
import org.esc.tasktracker.services.UsersService
import org.esc.tasktracker.unit.generators.createLoginUserDto
import org.esc.tasktracker.unit.generators.createUser
import org.esc.tasktracker.unit.generators.createUserCreateDto
import org.esc.tasktracker.unit.generators.updateUsersTokensDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*
import kotlin.test.Test

class AuthServiceTest {

    private var usersService = mockk<UsersService>()
    private var jwtUtil = mockk<JwtUtil>()
    private var passwordEncoder = mockk<PasswordEncoder>()
    private var defaultExceptionMessages = mockk<DefaultExceptionMessages>()

    private var service = AuthService(usersService, jwtUtil, passwordEncoder, defaultExceptionMessages)

    @BeforeEach
    fun setup() {
        every { defaultExceptionMessages.userDoubleRecord() } returns "The user with this email already exists."
        every { defaultExceptionMessages.wrongUsernameOrPassword() } returns "Invalid username or password."
        every { defaultExceptionMessages.wrongRefreshToken() } returns "Invalid refresh token."
    }

    @Nested
    inner class RegisterTests {

        @Test
        fun `should register user and return tokens`() {
            val createUserDto = createUserCreateDto()
            val user = createUser()
            val uuid = UUID.randomUUID()

            every { usersService.create(createUserDto) } returns user

            /** private generateTokens(user: Users, sessionId: UUID) func mocks */
            every { jwtUtil.removeOldRefreshTokenByUUID(uuid) } just runs
            every { jwtUtil.generateAccessToken(any()) } returns "accessToken"
            every { jwtUtil.generateRefreshToken(any()) } returns "refreshToken"

            val result = service.register(createUserDto, uuid)

            assertEquals("accessToken", result.accessToken)
            assertEquals("refreshToken", result.refreshToken)

            verify {
                usersService.create(createUserDto)
                jwtUtil.removeOldRefreshTokenByUUID(uuid)
                jwtUtil.generateAccessToken(any())
                jwtUtil.generateRefreshToken(any())
            }
        }

        @Test
        fun `should throw exception when user already exists`() {
            val createUserDto = createUserCreateDto()
            val uuid = UUID.randomUUID()

            every { usersService.create(createUserDto) } throws DoubleRecordException(defaultExceptionMessages.userDoubleRecord())

            assertThrows<DoubleRecordException> { service.register(createUserDto, uuid) }

            verify { usersService.create(createUserDto) }
            verify(exactly = 0) {
                jwtUtil.removeOldRefreshTokenByUUID(uuid)
                jwtUtil.generateAccessToken(any())
                jwtUtil.generateRefreshToken(any())
            }
        }
    }

    @Nested
    inner class LoginTests {

        @Test
        fun `should login user and return tokens`() {
            val user = createUser()
            val uuid = UUID.randomUUID()
            val loginDto = createLoginUserDto(user.email, user.password)

            every { usersService.getByEmail(any(), any()) } returns user
            every { passwordEncoder.matches(any(), any()) } returns true

            /** private generateTokens(user: Users, sessionId: UUID) func mocks */
            every { jwtUtil.removeOldRefreshTokenByUUID(uuid) } just runs
            every { jwtUtil.generateAccessToken(any()) } returns "accessToken"
            every { jwtUtil.generateRefreshToken(any()) } returns "refreshToken"

            val result = service.login(loginDto, uuid)

            assertEquals("accessToken", result.accessToken)
            assertEquals("refreshToken", result.refreshToken)

            verify {
                usersService.getByEmail(user.email, false)
                jwtUtil.removeOldRefreshTokenByUUID(uuid)
                jwtUtil.generateAccessToken(any())
                jwtUtil.generateRefreshToken(any())
            }
        }

        @Test
        fun `should throw exception when user's password is invalid`() {
            val user = createUser()
            val uuid = UUID.randomUUID()
            val loginDto = createLoginUserDto(user.email, "wrong_password")

            every { usersService.getByEmail(any(), any()) } returns user
            every { passwordEncoder.matches(any(), any()) } returns false

            val exception = assertThrows<JwtAuthenticationException> { service.login(loginDto, uuid) }

            assertEquals(exception.message, defaultExceptionMessages.wrongUsernameOrPassword())

            verify {
                usersService.getByEmail(user.email, false)
                passwordEncoder.matches(loginDto.password, user.password)
            }
            verify(exactly = 0) {
                jwtUtil.removeOldRefreshTokenByUUID(uuid)
                jwtUtil.generateAccessToken(any())
                jwtUtil.generateRefreshToken(any())
            }
        }

        @Test
        fun `should throw exception when user doesn't exists`() {
            val user = createUser()
            val uuid = UUID.randomUUID()
            val loginDto = createLoginUserDto("wrong@mail.ru", user.password)

            every { usersService.getByEmail(any(), any()) } returns null

            val exception = assertThrows<JwtAuthenticationException> { service.login(loginDto, uuid) }

            assertEquals(exception.message, defaultExceptionMessages.wrongUsernameOrPassword())

            verify { usersService.getByEmail(loginDto.email, false) }
            verify(exactly = 0) {
                passwordEncoder.matches(user.password, user.password)
                jwtUtil.removeOldRefreshTokenByUUID(uuid)
                jwtUtil.generateAccessToken(any())
                jwtUtil.generateRefreshToken(any())
            }
        }
    }

    @Nested
    inner class UpdateTokensTests {
        @Test
        fun `should validate old tokens and return new tokens`() {
            val dto = updateUsersTokensDto()
            val uuid = UUID.randomUUID()
            val user = createUser()

            every { jwtUtil.verifyToken(dto.accessToken, any<UUID>(), throwTimeLimit = false) } returns false
            every { jwtUtil.verifyToken(dto.refreshToken, any<UUID>(), throwTimeLimit = true) } returns true
            every { jwtUtil.getUserFromToken(any()) } returns user

            /** private generateTokens(user: Users, sessionId: UUID) func mocks */
            every { jwtUtil.removeOldRefreshTokenByUUID(uuid) } just runs
            every { jwtUtil.generateAccessToken(any()) } returns "accessToken"
            every { jwtUtil.generateRefreshToken(any()) } returns "refreshToken"

            val result = service.updateTokens(dto, uuid)

            assertEquals("accessToken", result.accessToken)
            assertEquals("refreshToken", result.refreshToken)

            verify(exactly = 2) { jwtUtil.verifyToken(any(), any<UUID>(), throwTimeLimit = any<Boolean>()) }
            verify {
                jwtUtil.getUserFromToken(any())
                jwtUtil.removeOldRefreshTokenByUUID(uuid)
                jwtUtil.generateAccessToken(any())
                jwtUtil.generateRefreshToken(any())
            }
        }

        @Test
        fun `should throw exception when refresh token is expired`() {
            val dto = updateUsersTokensDto()
            val uuid = UUID.randomUUID()

            every { jwtUtil.verifyToken(dto.accessToken, any<UUID>(), throwTimeLimit = false) } returns false
            every {
                jwtUtil.verifyToken(
                    dto.refreshToken,
                    any<UUID>(),
                    throwTimeLimit = true
                )
            } throws JwtAuthenticationException("Срок жизни токена истёк.")

            val exception = assertThrows<JwtAuthenticationException> { service.updateTokens(dto, uuid) }

            assertEquals(exception.message, "Срок жизни токена истёк.")

            verify(exactly = 2) { jwtUtil.verifyToken(any(), any<UUID>(), throwTimeLimit = any<Boolean>()) }
            verify(exactly = 0) {
                jwtUtil.getUserFromToken(any())
                jwtUtil.removeOldRefreshTokenByUUID(uuid)
                jwtUtil.generateAccessToken(any())
                jwtUtil.generateRefreshToken(any())
            }
        }

        @Test
        fun `should throw exception when tokens are valid, but user doesn't exist`() {
            val dto = updateUsersTokensDto()
            val uuid = UUID.randomUUID()

            every { jwtUtil.verifyToken(dto.accessToken, any<UUID>(), throwTimeLimit = false) } returns false
            every { jwtUtil.verifyToken(dto.refreshToken, any<UUID>(), throwTimeLimit = true) } returns true
            every { jwtUtil.getUserFromToken(any()) } returns null

            val exception = assertThrows<JwtAuthenticationException> { service.updateTokens(dto, uuid) }

            assertEquals(exception.message, defaultExceptionMessages.wrongRefreshToken())

            verify(exactly = 2) { jwtUtil.verifyToken(any(), any<UUID>(), throwTimeLimit = any<Boolean>()) }
            verify { jwtUtil.getUserFromToken(any()) }
            verify(exactly = 0) {
                jwtUtil.removeOldRefreshTokenByUUID(uuid)
                jwtUtil.generateAccessToken(any())
                jwtUtil.generateRefreshToken(any())
            }
        }
    }
}