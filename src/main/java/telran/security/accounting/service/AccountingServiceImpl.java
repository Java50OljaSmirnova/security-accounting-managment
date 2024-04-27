package telran.security.accounting.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import telran.exceptions.AccountIllegalStateException;
import telran.exceptions.AccountNotFoundException;
import telran.probes.dto.AccountDto;
import telran.security.accounting.model.Account;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountingServiceImpl implements AccountingService {
	private static final String HASH_PASSWORD_FIELD = "hashPassword";
	final MongoTemplate mongoTemplate;
	final PasswordEncoder passwordEncoder;

	@Override
	public AccountDto addAccount(AccountDto accountDto) {
		System.out.println(SecurityContextHolder.getContext().getAuthentication().getName());
		String email = accountDto.email();
		Account account = null;
		AccountDto encodedAccount = getEncoded(accountDto);
		try {
			account = mongoTemplate.insert(Account.of(encodedAccount));
		}catch(DuplicateKeyException e) {
			throw new AccountIllegalStateException(email);
		}
		log.debug("account {} has been saved", email);
		return account.build();
	}

	private AccountDto getEncoded(AccountDto accountDto) {
		
		return new AccountDto(accountDto.email(), passwordEncoder.encode(accountDto.password()), accountDto.roles());
	}

	
	@Override
	public AccountDto removeAccount(String email) {
		Account account = mongoTemplate.findAndRemove(queryByEmail(email), Account.class);
		if(account == null) {
			throw new AccountNotFoundException(email);
		}
		log.debug("account {} has been removed", email);
		return account.build();
	}
	private Query queryByEmail(String email) {
		return new Query(Criteria.where("email").is(email));
	}

	@Override
	public void updatePassword(String email, String newPassword) {
		String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
		if(!currentUser.equals(email)) {
			throw new IllegalArgumentException("Username mismatching");
		}
		Update update = new Update();
		String hashPassword = passwordEncoder.encode(newPassword);
		update.set(HASH_PASSWORD_FIELD, hashPassword);
		Account account = mongoTemplate.findAndModify(queryByEmail(email), update, Account.class);
		if(account == null) {
			throw new AccountNotFoundException(email);
		}
		log.debug("password of account {} has been updated", email);
	}
}
