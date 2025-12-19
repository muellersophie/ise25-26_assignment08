package de.seuhd.campuscoffee.domain.implementation;

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException;
import de.seuhd.campuscoffee.domain.model.objects.User;
import de.seuhd.campuscoffee.domain.ports.data.CrudDataService;
import de.seuhd.campuscoffee.domain.tests.TestFixtures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CrudServiceTest {

    @Mock
    private CrudDataService<User, Long> dataService;

    private CrudServiceImpl<User, Long> crudService;

    private static class TestCrudService extends CrudServiceImpl<User, Long> {

        private final CrudDataService<User, Long> dataService;

        TestCrudService(CrudDataService<User, Long> dataService) {
            super(User.class);
            this.dataService = dataService;
        }

        @Override
        protected CrudDataService<User, Long> dataService() {
            return dataService;
        }
    }

    @BeforeEach
    void setUp() {
        crudService = new TestCrudService(dataService);
    }

    @Test
    void clearObjectsFromDataService() {
        crudService.clear();
        verify(dataService).clear();
    }

    @Test
    void getAllObjectsFromDataService() {
        List<User> users = TestFixtures.getUserFixtures();
        when(dataService.getAll()).thenReturn(users);
        List<User> result = crudService.getAll();
        assertThat(result).hasSize(users.size());

        verify(dataService).getAll();
    }

    @Test
    void getByIdReturnsObject() {
        User user = TestFixtures.getUserFixtures().getFirst();
        Assertions.assertNotNull(user.getId());

        when(dataService.getById(user.getId())).thenReturn(user);

        User result = crudService.getById(user.getId());

        assertThat(result).isEqualTo(user);
        verify(dataService).getById(user.getId());
    }

    @Test
    void upsertCreatesNewObjectWhenIdIsNull() {
        User newUser = TestFixtures.getUserFixturesForInsertion().getFirst();
        User createdUser = newUser.toBuilder().id(4L).build();
        when(dataService.upsert(newUser)).thenReturn(createdUser);

        User result = crudService.upsert(newUser);
        assertThat(result.getId()).isEqualTo(4L);

        verify(dataService).upsert(newUser);
        verify(dataService, never()).getById(any());
    }

    @Test
    void upsertUpdatesExistingEntityWhenIdIsPresent() {
        User existingUser = TestFixtures.getUserFixtures().getFirst();
        Assertions.assertNotNull(existingUser.getId());

        when(dataService.getById(existingUser.getId())).thenReturn(existingUser);
        when(dataService.upsert(existingUser)).thenReturn(existingUser);

        User updatedUser = crudService.upsert(existingUser);

        assertThat(updatedUser).isEqualTo(existingUser);

        verify(dataService).getById(existingUser.getId());

        verify(dataService).upsert(existingUser);
    }

    @Test
    void upsertEntityDoesNotExistException() {
        User user = TestFixtures.getUserFixtures().getFirst();
        Assertions.assertNotNull(user.getId());
        when(dataService.getById(user.getId())).thenThrow(new NotFoundException(User.class, user.getId()));
        assertThrows(NotFoundException.class, () -> crudService.upsert(user));
        verify(dataService).getById(user.getId());
        verify(dataService, never()).upsert(any());
    }

    @Test
    void deleteDelegatesToDataService() {
        Long userId = TestFixtures.getUserFixtures().getFirst().getId();
        Assertions.assertNotNull(userId);
        crudService.delete(userId);

        verify(dataService).delete(userId);
    }
}
