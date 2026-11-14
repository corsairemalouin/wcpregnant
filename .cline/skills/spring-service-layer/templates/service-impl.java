package org.manu.wcpregnant.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.bdd.mapper.XxxDAOMapper;
import org.manu.wcpregnant.bdd.repository.XxxRepository;
import org.manu.wcpregnant.service.XxxService;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Implémentation de {@link XxxService}.
 *
 * <p>Dépend uniquement d'abstractions ({@link XxxRepository}, {@link XxxDAOMapper})
 * conformément au principe DIP.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class XxxServiceImpl implements XxxService {

    private final XxxRepository repository;
    private final XxxDAOMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<XxxDto> findAll(Integer page, Integer size) {
        Objects.requireNonNull(page, "page must not be null");
        Objects.requireNonNull(size, "size must not be null");
        log.debug("Fetching xxx page {} size {}", page, size);
        return repository.findAll(PageRequest.of(page, size))
                .map(mapper::daoToBo)
                .getContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XxxDto findById(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id)
                .map(mapper::daoToBo)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }
}
