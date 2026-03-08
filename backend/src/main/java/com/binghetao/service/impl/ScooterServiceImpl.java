package com.binghetao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.binghetao.domain.Scooter;
import com.binghetao.mapper.ScooterMapper;
import com.binghetao.service.ScooterService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScooterServiceImpl extends ServiceImpl<ScooterMapper, Scooter> implements ScooterService {

    @Override
    public List<Scooter> listAll() {
        return baseMapper.selectList(null);
    }

    @Override
    public boolean addScooter(Scooter scooter) {
        if (scooter == null || scooter.getScooterCode() == null || scooter.getScooterCode().isBlank()) {
            return false;
        }

        LambdaQueryWrapper<Scooter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Scooter::getScooterCode, scooter.getScooterCode());
        Long count = baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return false;
        }

        if (scooter.getStatus() == null || scooter.getStatus().isBlank()) {
            scooter.setStatus("AVAILABLE");
        }
        return baseMapper.insert(scooter) > 0;
    }

    @Override
    public boolean updateScooter(Scooter scooter) {
        if (scooter == null || scooter.getId() == null) {
            return false;
        }
        Scooter existing = baseMapper.selectById(scooter.getId());
        if (existing == null) {
            return false;
        }

        // 如果修改业务编号，需要检查唯一性
        String scooterCode = scooter.getScooterCode();
        if (scooterCode != null && !scooterCode.equals(existing.getScooterCode())) {
            LambdaQueryWrapper<Scooter> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Scooter::getScooterCode, scooterCode)
                    .ne(Scooter::getId, scooter.getId());
            Long count = baseMapper.selectCount(wrapper);
            if (count != null && count > 0) {
                return false;
            }
            existing.setScooterCode(scooterCode);
        }

        if (scooter.getStatus() != null) {
            existing.setStatus(scooter.getStatus());
        }
        if (scooter.getLocation() != null) {
            existing.setLocation(scooter.getLocation());
        }

        return baseMapper.updateById(existing) > 0;
    }

    @Override
    public boolean deleteScooter(Long id) {
        return baseMapper.deleteById(id) > 0;
    }
}

