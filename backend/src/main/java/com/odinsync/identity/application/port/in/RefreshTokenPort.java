package com.odinsync.identity.application.port.in;

import com.odinsync.identity.application.command.RefreshTokenCommand;
import com.odinsync.identity.application.usecase.RefreshTokenResult;

public interface RefreshTokenPort {

	RefreshTokenResult refresh(RefreshTokenCommand command);
}
