package com.game.service;

import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.exception.BadRequestException;
import com.game.exception.NotFoundException;
import com.game.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class PlayerService {

    @Autowired
    private PlayerRepository repository;

    private static final int MAX_LENGTH_NAME = 12;
    private static final int MAX_LENGTH_TITLE = 30;
    private static final int MAX_SIZE_EXPERIENCE = 10000000;
    private static final long MIN_BIRTHDAY = 2000L;
    private static final long MAX_BIRTHDAY = 3000L;

    public Integer calcCurrentLevel (Integer exp) {
        return (int)(Math.sqrt(200 * exp + 2500) - 50) / 100;
    }

    public Integer calcNecessaryExperienceForNextLevel (Integer exp, Integer lvl) {
        return 50 * (lvl + 1) * (lvl + 2) - exp;
    }

    public Page<Player> findAllPlayers(Specification<Player> specification, Pageable pageable) {
        return repository.findAll(specification, pageable);
    }

    public Long getCountPlayers(Specification<Player> specification) {
        return repository.count(specification);
    }

    public Player createPlayer(Player player) {
        checkName(player.getName());
        checkTitle(player.getTitle());
        checkRace(player.getRace());
        checkProfession(player.getProfession());
        checkBirthday(player.getBirthday());
        checkExperience(player.getExperience());
        if(player.getBanned() == null) player.setBanned(false);
        player.setLevel(calcCurrentLevel(player.getExperience()));
        player.setUntilNextLevel(calcNecessaryExperienceForNextLevel(player.getExperience(), player.getLevel()));
        return repository.saveAndFlush(player);
    }

    public Player getOnePlayerByID(Long id) {
        checkID(id);
        return repository.findById(id).orElseThrow(() -> new NotFoundException("No player with this ID! Error 404!"));
    }

    public Player updatePlayer(Long id, Player player) {
        Player newPlayer = getOnePlayerByID(id);

        if(player.getName() != null) {
            checkName(player.getName());
            newPlayer.setName(player.getName());
        }

        if(player.getTitle() != null) {
            checkTitle(player.getTitle());
            newPlayer.setTitle(player.getTitle());
        }

        if(player.getRace() != null) {
            checkRace(player.getRace());
            newPlayer.setRace(player.getRace());
        }

        if(player.getProfession() != null) {
            checkProfession(player.getProfession());
            newPlayer.setProfession(player.getProfession());
        }

        if(player.getBirthday() != null) {
            checkBirthday(player.getBirthday());
            newPlayer.setBirthday(player.getBirthday());
        }

        if(player.getBanned() != null) {
            newPlayer.setBanned(player.getBanned());
        }

        if(player.getExperience() != null) {
            checkExperience(player.getExperience());
            newPlayer.setExperience(player.getExperience());
        }

        newPlayer.setLevel(calcCurrentLevel(newPlayer.getExperience()));
        newPlayer.setUntilNextLevel(calcNecessaryExperienceForNextLevel(newPlayer.getExperience(), newPlayer.getLevel()));

        return repository.save(newPlayer);
    }

    public void delete(Long id) {
        Player player = getOnePlayerByID(id);
        repository.delete(player);
    }

    public void checkID(Long id) {
        if(id <= 0) throw new BadRequestException("Invalid ID! The ID must be greater than zero!");
    }

    public void checkName(String name) {
        if(name == null || name.isEmpty() || name.length() > MAX_LENGTH_NAME)
            throw new BadRequestException("Invalid Name! The field cannot be empty and has a maximum size of 12 characters");
    }

    public void checkTitle(String title) {
        if(title.length() > MAX_LENGTH_TITLE || title == null || title.isEmpty())
            throw new BadRequestException("Invalid Title! The field cannot be empty and has a maximum size of 30 characters");
    }

    public void checkRace(Race race) {
        if(race == null)
            throw new BadRequestException("Invalid Race!");
    }

    public void checkProfession(Profession profession) {
        if(profession == null)
            throw new BadRequestException("Invalid Profession!");
    }

    public void checkBirthday(Date birthday) {
        if(birthday == null)
            throw new BadRequestException("Invalid Birthday!");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(birthday.getTime());
        if (calendar.get(Calendar.YEAR) < MIN_BIRTHDAY || calendar.get(Calendar.YEAR) > MAX_BIRTHDAY)
            throw new BadRequestException("Birthday goes beyond what is bounds");
    }

    public void checkExperience(Integer experience) {
        if(experience < 0 || experience > MAX_SIZE_EXPERIENCE || experience == null)
            throw new BadRequestException("Invalid Experience!");
    }

    public Specification<Player> filterName(String name) {

        return (root, query, cb) -> name == null ? null:cb.like(root.get("name"),"%"+name+"%");
    }

    public Specification<Player> filterTitle(String title) {
        return (root, query, cb) -> title == null ? null:cb.like(root.get("title"),"%"+title+"%");
    }

    public Specification<Player> filterRace(Race race) {
        return (root, query, cb) -> race == null ? null:cb.equal(root.get("race"),race);
    }

    public Specification<Player> filterProfession(Profession profession) {
        return (root, query, cb) -> profession == null ? null:cb.equal(root.get("profession"), profession);
    }

    public Specification<Player> filterExperience(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("experience"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("experience"), min);
            return cb.between(root.get("experience"), min, max);
        };
    }

    public Specification<Player> filterLevel(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("level"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("level"), min);
            return cb.between(root.get("level"), min, max);
        };
    }

    public Specification<Player> filterBirthday(Long after, Long before) {
        return (root, query, cb) -> {
            if (after == null && before == null) return null;
            if (after == null) return cb.lessThanOrEqualTo(root.get("birthday"), new Date(before));
            if (before == null) return cb.greaterThanOrEqualTo(root.get("birthday"), new Date(after));
            return cb.between(root.get("birthday"), new Date(after), new Date(before));
        };
    }

    public Specification<Player> filterBanned(Boolean isBanned) {
        return (root, query, cb)->{
            if (isBanned == null) return null;
            if (isBanned) return cb.isTrue(root.get("banned"));
            return cb.isFalse(root.get("banned"));
        };
    }


}
