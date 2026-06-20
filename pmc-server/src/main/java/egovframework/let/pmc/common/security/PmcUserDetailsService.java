package egovframework.let.pmc.common.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 웹 UI 로그인용 UserDetailsService.
 * comtnemplyrinfo + comtnempauthor 기반, 계정 잠금/상태 반영.
 */
@Service("userDetailsService")
public class PmcUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Autowired
    public PmcUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Map<String, Object> u = userMapper.selectUser(username);
        if (u == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        }
        List<String> roles = userMapper.selectAuthorities(username);
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            for (String r : roles) {
                if (r != null && !r.trim().isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority(r));
                }
            }
        }
        boolean locked = "Y".equals(String.valueOf(u.get("lockAt")));
        boolean enabled = "P".equals(String.valueOf(u.get("emplyrSttus")));
        return User.withUsername(username)
                .password(String.valueOf(u.get("password")))
                .authorities(authorities)
                .accountLocked(locked)
                .disabled(!enabled)
                .build();
    }
}
