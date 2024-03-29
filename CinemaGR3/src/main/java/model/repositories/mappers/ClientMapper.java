package model.repositories.mappers;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;
import model.repositories.daos.ClientDao;

@Mapper
public interface ClientMapper {

    @DaoFactory
    ClientDao clientDao();
}
