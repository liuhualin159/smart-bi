package dev.lhl.metadata.mapper;

import dev.lhl.metadata.domain.BusinessDomain;
import java.util.List;

/**
 * 业务域Mapper接口
 * 
 * @author smart-bi
 */
public interface BusinessDomainMapper
{
    public BusinessDomain selectBusinessDomainById(Long id);
    public List<BusinessDomain> selectBusinessDomainList(BusinessDomain businessDomain);
    public int insertBusinessDomain(BusinessDomain businessDomain);
    public int updateBusinessDomain(BusinessDomain businessDomain);
    public int deleteBusinessDomainById(Long id);
    public int deleteBusinessDomainByIds(Long[] ids);
}
