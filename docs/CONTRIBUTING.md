## Branch Strategy
- feature/* : 기능 개발
- chore/*   : 설정/규칙/메타 작업
- fix/*     : 버그 수정

## Commit Message
- feat: 기능 추가
- fix: 버그 수정
- chore: 설정/문서/빌드
- refactor: 리팩터링

## Pull Request
- PR 템플릿 필수 사용
- 테스트 결과 포함

## How To
1. **Clone repository from remote repository**
    - 관리자가 생성한 Repository Clone
2. **Create feature branch & implement your code**
    - 기능 브랜치에서 코드 구현 및 완료
    - Commit 종료 이후 항상 clean 상태 확인

    ```
    (develop) $ git flow feature start [name]
    (develop) $ git status
    ```

3. **Publish feature branch to remote repository**
    - 로컬 작업 브랜치 원격에 push

    ```
    (feature) $ git flow feature publish [name]
    ```

4. **Create pull request from feature branch to develop branch**
    - 풀리퀘스트 생성(feature → develop)
    - 방향 및 master 브랜치에 풀리퀘스트를 생성하지 않도록 주의
5. **Merge pull request**
    - 코드 리뷰 및 코드 병합(3가지 옵션중 단순 merge 사용)
6. **Synchronize your remote repository and local repository**
    - 반영된 본인의 코드를 로컬에 최신화
    - 기능 브랜치 종료

    ```
    (develop) $ git fetch origin
    (develop) $ git merge origin/develop
    (feature) $ git flow feature finish [name]
    ```

7. **Clean up branches**
    - 다른사람의 기능 브랜치 이력 제거

    ```
    $ git remote prune origin
    ```


**풀리퀘스트가 여러 개인 경우**

1. **Merge first pull request**
    - 첫 번째 풀리퀘스트가 병합된 이후 시나리오
2. **Merge updated develop branch**
    - 이전 풀리퀘스트가 병합된 개발 브랜치를 로컬에 반영

    ```
    (develop) $ git fetch origin
    (develop) $ git merge origin/develop
    ```

3. **Rebase your code from updated develop branch**
    - 현재 기능 브랜치에 최신화된 개발 브랜치를 반영

    ```
    (feature) git rebase -i develop
    ```

4. **Resolve conflict (반복)**
    - 충돌 해결

    ```
    **코드 충돌 해결**
    (commit hash) $ git add . 
    (commit hash) $ git rebase --continue
    ```

5. **Push your feature branch by force**
    - 풀리퀘스트에서 추적중인 브랜치 최신화를 위해 rebase 된 기능 브랜치를 강제로 push

    ```
    (feature) $ git push origin feature/[name] --force
    ```

6. **Merge pull request**
    - 코드 리뷰 및 코드 병합
7. **Synchronize your remote repository and local repository**
    - 반영된 본인의 코드를 로컬에 최신화
    - 기능 브랜치 종료

    ```
    (develop) $ git fetch origin
    (develop) $ git merge origin/develop
    (feature) $ git flow feature finish [name]
    ```

8. **Clean up branches**
    - 다른사람의 기능 브랜치 이력 제거

    ```
    $ git remote prune origin
    ```