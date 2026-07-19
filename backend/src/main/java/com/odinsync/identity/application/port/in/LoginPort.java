package com.odinsync.identity.application.port.in;

import com.odinsync.identity.application.command.LoginCommand;
import com.odinsync.identity.application.model.LoginResult;

public interface LoginPort {

	LoginResult login(LoginCommand command);
}
