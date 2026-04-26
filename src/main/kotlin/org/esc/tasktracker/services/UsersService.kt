package org.esc.tasktracker.services

import org.esc.tasktracker.dto.filters.UsersFilterDto
import org.esc.tasktracker.dto.users.CreateUserDto
import org.esc.tasktracker.dto.users.UpdateUserDto
import org.esc.tasktracker.entities.Users
import org.esc.tasktracker.exceptions.DoubleRecordException
import org.esc.tasktracker.exceptions.NotFoundException
import org.esc.tasktracker.extensions.takeIfOrThrow
import org.esc.tasktracker.interfaces.CrudService
import org.esc.tasktracker.mappers.UsersMapper
import org.esc.tasktracker.repositories.UsersRepository
import org.esc.tasktracker.repositories.specs.UsersSpecifications
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service responsible for managing user operations including CRUD operations and user-specific queries.
 *
 * This service implements [CrudService] for standard CRUD operations and provides additional
 * functionality for user management such as email-based lookups and password encoding.
 * It handles user data persistence, validation, and mapping between DTOs and entity objects.
 *
 * @param repository The repository for [Users] entity data access
 * @param usersSpecifications Provides specification builders for filtering users
 * @param usersMapper Maps between user DTOs and entity objects
 * @param passwordEncoder Encodes and verifies user passwords
 *
 * @property repository The injected repository instance for data access operations
 * @property usersSpecifications The injected specifications instance for query building
 * @property usersMapper The injected mapper instance for DTO-entity conversions
 * @property passwordEncoder The injected password encoder instance
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
@Service
open class UsersService(
    override val repository: UsersRepository,
    private val usersSpecifications: UsersSpecifications,
    private val usersMapper: UsersMapper,
    private val passwordEncoder: PasswordEncoder,
) : CrudService<Users, Long, CreateUserDto, UpdateUserDto, UsersFilterDto> {

    /**
     * Retrieves a paginated list of users based on the provided filters.
     *
     * This method builds dynamic specifications based on the filter criteria:
     * - Filters by name if provided in the filter DTO
     * - Filters by email if provided in the filter DTO
     * All specified filters are combined using AND logic.
     *
     * @param filters Optional filter criteria containing name and/or email patterns
     * @param pageable Pagination information including page number, size, and sort order
     * @return A [Page] of [Users] entities matching the filter criteria
     */
    override fun getAll(
        filters: UsersFilterDto?,
        pageable: Pageable,
    ): Page<Users> {
        val specs = listOfNotNull(
            usersSpecifications.hasName(filters?.name),
            usersSpecifications.hasEmail(filters?.email)
        )

        return repository.findAll(Specification.allOf(specs), pageable)
    }

    /**
     * Retrieves a user by their email address.
     *
     * This method performs a case-sensitive lookup of a user by email.
     * The behavior when no user is found can be controlled via the [throwable] parameter.
     *
     * @param email The email address to search for
     * @param throwable If true, throws [NotFoundException] when user not found;
     *                  if false, returns null when user not found
     * @return The [Users] entity if found, or null if not found and [throwable] is false
     * @throws NotFoundException If user not found and [throwable] is true
     */
    fun getByEmail(email: String, throwable: Boolean = true): Users? = repository.findByEmail(email)
        ?: if (throwable) throw NotFoundException("Пользователя с email $email не существует.") else null

    /**
     * Creates a new user with the provided data.
     *
     * This method performs the following operations transactionally:
     * 1. Validates that no user exists with the same email
     * 2. Maps the creation DTO to a user entity
     * 3. Encodes the plain text password
     * 4. Persists the new user to the database
     *
     * @param item The DTO containing user creation data (email, password, name)
     * @return The created [Users] entity with generated ID and encoded password
     * @throws DoubleRecordException If a user with the same email already exists
     */
    @Transactional
    override fun create(item: CreateUserDto): Users {
        getByEmail(
            item.email,
            throwable = false
        )?.let { throw DoubleRecordException("Пользователь с таким email уже существует.") }

        return repository.save(usersMapper.userFromDto(item).copy(password = passwordEncoder.encode(item.password)!!))
    }

    /**
     * Updates an existing user with the provided data.
     *
     * This method performs the following operations transactionally:
     * 1. Retrieves the existing user by ID
     * 2. Updates the name if provided in the DTO
     * 3. Updates the email if provided, validating that the new email is not already in use
     * 4. Saves the updated user entity
     *
     * Note: Password updates are not handled by this method and require a separate process.
     *
     * @param item The DTO containing user update data (ID and optional fields to update)
     * @return The updated [Users] entity
     * @throws NotFoundException If no user exists with the provided ID
     * @throws DoubleRecordException If the new email is already in use by another user
     */
    @Transactional
    override fun update(item: UpdateUserDto): Users {
        val user = getById(item.id, message = "Пользователь с ID ${item.id} не найден.")!!

        item.name?.let { user.name = it }
        item.email
            ?.takeIf { user.email != it }
            ?.takeIfOrThrow(
                predicate = { email -> getByEmail(email, false) == null },
                exception = { DoubleRecordException("Email ${item.email} занят") }
            )?.also { user.email = it }

        return repository.save(user)
    }

    /**
     * Deletes a user by their ID.
     *
     * This method transactionally removes a user from the database.
     * If the user doesn't exist, a [NotFoundException] is thrown.
     *
     * @param id The ID of the user to delete
     * @return A success message confirming the deletion
     * @throws NotFoundException If no user exists with the provided ID
     */
    @Transactional
    override fun deleteById(id: Long): String {
        getById(id, message = "Пользователь с ID $id не найден.")?.let {
            repository.deleteById(id)
        }

        return "Пользователь удален успешно."
    }

    /**
     * Deletes all users from the database.
     *
     * This method transactionally removes all user records.
     * Use with caution as this operation cannot be undone.
     *
     * @return A success message confirming the deletion of all users
     */
    @Transactional
    override fun deleteAll(): String {
        repository.deleteAll()

        return "Все пользователи удалены успешно."
    }
}