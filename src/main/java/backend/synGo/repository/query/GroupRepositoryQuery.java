package backend.synGo.repository.query;

import backend.synGo.form.GroupsPagingForm;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface GroupRepositoryQuery {

    Slice<GroupsPagingForm> findAllGroupForSlice(Pageable pageable);
}
