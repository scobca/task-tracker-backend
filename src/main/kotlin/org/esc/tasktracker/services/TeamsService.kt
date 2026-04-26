package org.esc.tasktracker.services

import org.esc.tasktracker.dto.filters.TeamsFilterDto
import org.esc.tasktracker.dto.teams.CreateTeamDto
import org.esc.tasktracker.dto.teams.CreateTeamMembershipDto
import org.esc.tasktracker.dto.teams.UpdateTeamDto
import org.esc.tasktracker.entities.Teams
import org.esc.tasktracker.enums.TeamRoles
import org.esc.tasktracker.interfaces.CrudService
import org.esc.tasktracker.mappers.TeamsMapper
import org.esc.tasktracker.repositories.TeamsRepository
import org.esc.tasktracker.repositories.specs.TeamsSpecifications
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service responsible for managing team operations including CRUD operations and team-specific queries.
 *
 * This service implements [CrudService] for standard CRUD operations and provides additional
 * functionality for team management such as retrieving teams by owner and handling team-user
 * relationships. It ensures data integrity by validating user existence before creating or
 * updating team ownership.
 *
 * @param repository The repository for [Teams] entity data access
 * @param teamsMapper Maps between team DTOs and entity objects
 * @param teamsSpecifications Provides specification builders for filtering teams
 * @param usersService Service for user validation and retrieval operations
 *
 * @property repository The injected repository instance for data access operations
 * @property teamsMapper The injected mapper instance for DTO-entity conversions
 * @property teamsSpecifications The injected specifications instance for query building
 * @property usersService The injected users service for user-related operations
 *
 * @author Vladimir Fokin
 * @since 0.2.0
 */
@Service
open class TeamsService(
    override val repository: TeamsRepository,
    private val teamsMapper: TeamsMapper,
    private val teamsSpecifications: TeamsSpecifications,
    private val usersService: UsersService,
    private val applicationEventPublisher: ApplicationEventPublisher
) :
    CrudService<Teams, Long, CreateTeamDto, UpdateTeamDto, TeamsFilterDto> {

    /**
     * Retrieves a paginated list of teams based on the provided filters.
     *
     * This method builds dynamic specifications based on the filter criteria:
     * - Filters by team name if provided in the filter DTO
     * - Filters by owner ID if provided in the filter DTO
     * All specified filters are combined using AND logic.
     *
     * @param filters Optional filter criteria containing team name and/or owner ID patterns
     * @param pageable Pagination information including page number, size, and sort order
     * @return A [Page] of [Teams] entities matching the filter criteria
     */
    override fun getAll(
        filters: TeamsFilterDto?,
        pageable: Pageable
    ): Page<Teams> {
        val specs = listOfNotNull(
            teamsSpecifications.hasName(filters?.name),
            teamsSpecifications.hasOwnerId(filters?.ownerId),
        )

        return repository.findAll(Specification.allOf(specs), pageable)
    }

    /**
     * Retrieves all teams owned by a specific user.
     *
     * This method first validates that the user exists, then returns a paginated list
     * of teams where the specified user is the owner. If the user doesn't exist,
     * a [org.esc.tasktracker.exceptions.NotFoundException] is thrown.
     *
     * @param userId The ID of the user whose teams to retrieve
     * @param pageable Pagination information including page number, size, and sort order
     * @return A [Page] of [Teams] entities owned by the specified user
     * @throws org.esc.tasktracker.exceptions.NotFoundException If no user exists with the provided ID
     */
    fun getByUserId(userId: Long, pageable: Pageable): Page<Teams> {
        return usersService.getById(userId, throwable = true, message = "Пользователь с ID $userId не найден.")!!
            .let { user -> repository.findByOwner(user, pageable) }
    }

    /**
     * Creates a new team with the provided data.
     *
     * This method performs the following operations transactionally:
     * 1. Validates that the owner user exists
     * 2. Maps the creation DTO to a team entity with the validated owner
     * 3. Persists the new team to the database
     *
     * @param item The DTO containing team creation data (name, optional description, owner ID)
     * @return The created [Teams] entity with generated ID
     * @throws org.esc.tasktracker.exceptions.NotFoundException If the specified owner user does not exist
     */
    @Transactional
    override fun create(item: CreateTeamDto): Teams {
        val user = usersService.getById(
            item.userId,
            throwable = true,
            message = "Пользователь с ID ${item.userId} не найден."
        )!!

        repository.save(teamsMapper.teamFromDto(item, user))
            .let { team ->
                applicationEventPublisher.publishEvent(
                    CreateTeamMembershipDto(
                        userId = user.id,
                        teamId = team.id,
                        role = TeamRoles.ADMIN
                    )
                )
                return team
            }
    }

    /**
     * Updates an existing team with the provided data.
     *
     * This method performs the following operations transactionally:
     * 1. Retrieves the existing team by ID
     * 2. Updates the name if provided (automatically trimmed)
     * 3. Updates the description if provided
     * 4. Updates the owner if provided, validating that the new owner user exists
     * 5. Saves the updated team entity
     *
     * @param item The DTO containing team update data (ID and optional fields to update)
     * @return The updated [Teams] entity
     * @throws org.esc.tasktracker.exceptions.NotFoundException If no team exists with the provided ID, or if the new owner user does not exist
     */
    @Transactional
    override fun update(item: UpdateTeamDto): Teams {
        val team = getById(item.id, message = "Команда с ID ${item.id} не найдена")!!

        item.name?.let { team.name = it.trim() }
        item.description?.let { team.description = it }
        item.userId?.let {
            val user = usersService.getById(
                it,
                throwable = true,
                message = "Пользователь с ID ${item.userId} не найден."
            )!!

            team.owner = user
        }

        return repository.save(team)
    }

    /**
     * Deletes a team by its ID.
     *
     * This method transactionally removes a team from the database.
     * If the team doesn't exist, a [org.esc.tasktracker.exceptions.NotFoundException] is thrown.
     *
     * @param id The ID of the team to delete
     * @return A success message confirming the deletion
     * @throws org.esc.tasktracker.exceptions.NotFoundException If no team exists with the provided ID
     */
    @Transactional
    override fun deleteById(id: Long): String {
        getById(id, message = "Команда с ID $id не найдена")?.let {
            repository.deleteById(id)
        }

        return "Команда удалена успешно."
    }

    /**
     * Deletes all teams from the database.
     *
     * This method transactionally removes all team records.
     * Use with caution as this operation cannot be undone and will cascade
     * to all related entities.
     *
     * @return A success message confirming the deletion of all teams
     */
    @Transactional
    override fun deleteAll(): String {
        repository.deleteAll()
        return "Все команды удалены успешно."
    }
}