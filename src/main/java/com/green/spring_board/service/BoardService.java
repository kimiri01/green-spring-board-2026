package com.green.spring_board.service;

import com.green.spring_board.dto.BoardResponse;
import com.green.spring_board.dto.BoardUpdateRequest;
import com.green.spring_board.dto.LikeDetailResponse;
import com.green.spring_board.entity.Like;
import com.green.spring_board.entity.User;
import com.green.spring_board.exceptions.AuthorizationFailureException;
import com.green.spring_board.exceptions.ResourceNotFoundException;
import com.green.spring_board.dto.BoardCreateRequest;
import com.green.spring_board.entity.Board;
import com.green.spring_board.exceptions.UnauthenticatedException;
import com.green.spring_board.repository.BoardRepository;
import com.green.spring_board.repository.LikeRepository;
import com.green.spring_board.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class BoardService {
    private BoardRepository boardRepository;
    private UserRepository userRepository;
    private LikeRepository likeRepository;

    // 전체 조회
    public List<BoardResponse> getAllBoards(int userId) {
        List<Board> boards = boardRepository.findAll();
        List<BoardResponse> boardResponses = new ArrayList<>();
        for (Board board : boards) {
            boardResponses.add(
                    new BoardResponse(
                            board.getId(),
                            board.getTitle(),
                            board.getContent(),
                            board.getHits(),
                            board.getLikeCount(),
                            (userId == -1) ? false : likeRepository.existsByUserIdAndBoardId(userId, board.getId()),
                            board.getUser().getId(),
                            board.getUser().getNickname(),
                            board.getCreatedDatetime(),
                            board.getUpdatedDatetime()
                    )
            );
        }
        return boardResponses;
    }

    // 상세 조회
    public BoardResponse getBoard(int id, int userId) {
        Optional<Board> optionalBoard = boardRepository.findById(id);
        if(optionalBoard.isEmpty()) {
            throw new ResourceNotFoundException("요청한 게시글을 찾지못했습니다.");
        }
        Board board = optionalBoard.get();

        User user = board.getUser();
        System.out.println(user.getNickname());
        board.setHits(board.getHits() + 1);
        boardRepository.save(board);
        return new BoardResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getHits(),
                board.getLikeCount(),
                (userId == -1) ? false : likeRepository.existsByUserIdAndBoardId(userId, board.getId()),
                board.getUser().getId(),
                board.getUser().getNickname(),
                board.getCreatedDatetime(),
                board.getUpdatedDatetime()
        );
    }

    // 내 게시글 조회
    public List<BoardResponse> getMyBoards(int userId) {
        List<Board> boards = boardRepository.findByUserId(userId);

        List<BoardResponse> boardResponses = new ArrayList<>();

        for (Board board : boards) {
            boardResponses.add(
                    new BoardResponse(
                            board.getId(),
                            board.getTitle(),
                            board.getContent(),
                            board.getHits(),
                            board.getLikeCount(),
                            likeRepository.existsByUserIdAndBoardId(userId, board.getId()),
                            board.getUser().getId(),
                            board.getUser().getNickname(),
                            board.getCreatedDatetime(),
                            board.getUpdatedDatetime()
                    )
            );
        }
        return boardResponses;
    }


    public int createBoard(BoardCreateRequest boardCreateRequest, int userId) {
        //* UserId 유효성 체크 (해당 userId의 유저가 정상적으로 존재하는지)
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new UnauthenticatedException("로그인한 사용자를 찾을 수 없습니다.");
        }

        Board board = new Board();
        board.setTitle(boardCreateRequest.getTitle());
        board.setContent(boardCreateRequest.getContent());
        board.setUser(user.get());
        Board saveBoard = boardRepository.save(board);

        return saveBoard.getId();
    }

    public void updateBoard(int id, BoardUpdateRequest boardUpdateRequest, int userId) {

        Optional<Board> optionalBoards = boardRepository.findById(id);
        if(optionalBoards.isEmpty()){
            // 게시글을 못찾은 경우
            throw new ResourceNotFoundException("게시글을 찾을 수 없습니다.");
        }
        Board board = optionalBoards.get();

        //* 작성자와 요청자 동일 여부 확인
        if(board.getUser().getId() != userId) {
            throw new AuthorizationFailureException("게시글 작업 권한이 없습니다.");
        }

        if(boardUpdateRequest.getTitle() != null && !boardUpdateRequest.getTitle().isBlank()) {
            board.setTitle(boardUpdateRequest.getTitle());
        }
        if(boardUpdateRequest.getContent() != null && !boardUpdateRequest.getContent().isBlank()) {
            board.setContent(boardUpdateRequest.getContent());
        }

        boardRepository.save(board);
    }

    public void deleteBoard(int id, int userId) {
        Optional<Board> optionalBoard = boardRepository.findById(id);
        if(optionalBoard.isEmpty()) {
            throw new ResourceNotFoundException("게시글을 찾을 수 없습니다.");
        }
        Board board = optionalBoard.get();

        if(board.getUser().getId() != userId) {
            throw new AuthorizationFailureException("게시글 작업 권한이 없습니다.");
        }

        boardRepository.deleteById(id);
    }

    // 좋아요
    public void pressLike(int id, int userId) {
        Optional<Board> optionalBoard = boardRepository.findById(id);
        if(optionalBoard.isEmpty()) {
            throw new ResourceNotFoundException("존재하지않는 게시글입니다.");
        }
        Board board = optionalBoard.get();

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("존재하지않는 유저입니다.");
        }
        User user = optionalUser.get();

        Optional<Like> likeOptional = likeRepository.findByUserIdAndBoardId(userId, id);
        if(likeOptional.isEmpty()) {
            //* 없으면 좋아요 추가
            Like like = new Like();
            like.setUser(user);
            like.setBoard(board);
            likeRepository.save(like);

            board.setLikeCount(board.getLikeCount() + 1);
            boardRepository.save(board);
        } else {
            //* 있으면 좋아요 삭제
            Like like = likeOptional.get();
            likeRepository.deleteById(like.getId());

            board.setLikeCount(board.getLikeCount() - 1);
            boardRepository.save(board);
        }
    }

    public LikeDetailResponse getLikeDetail(int id) {
        //* 1. 이 게시글의 좋아요 누른 유저 정보르들을 Like 테이블에서 싹다 가져옴
        List<Like> likes = likeRepository.findByBoardId(id);
        //* 2. 걔네 닉네임 하나하나 뽑아서, likeDetailResponse 에 집어넣음
        LikeDetailResponse likeDetailResponse = new LikeDetailResponse();
        List<String> nicknames = new ArrayList<>();
        for(Like like : likes) {
            nicknames.add(like.getUser().getNickname());
        }
        likeDetailResponse.setLikedUserNames(nicknames);
        //* 3. 끝
        return likeDetailResponse;
    }
}
