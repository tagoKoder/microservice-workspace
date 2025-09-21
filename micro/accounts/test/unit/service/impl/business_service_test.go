package impl_test

import (
	"context"
	"errors"
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/tagoKoder/accounts/internal/domain/model"
	"github.com/tagoKoder/accounts/internal/repository"
	"github.com/tagoKoder/accounts/internal/repository/uow"
	"github.com/tagoKoder/accounts/internal/service/impl"
)

// fakes
type fakeWriteRepo struct {
	created *model.Business
	err     error
}

func (f *fakeWriteRepo) Create(_ context.Context, b *model.Business) error {
	b.ID = 123
	f.created = b
	return f.err
}
func (f *fakeWriteRepo) UpdateCore(context.Context, *model.Business) error { return nil }
func (f *fakeWriteRepo) SoftDelete(context.Context, int64) error           { return nil }
func (f *fakeWriteRepo) Restore(context.Context, int64) error              { return nil }

type fakeReadRepo struct{}

func (f *fakeReadRepo) GetByID(_ context.Context, id int64) (*model.Business, error) {
	return &model.Business{ID: id, Name: "acme", GovernmentID: "RUC1"}, nil
}
func (f *fakeReadRepo) GetByGovernmentID(context.Context, string) (*model.Business, error) {
	return nil, nil
}

// fake UoW / QueryWork
type fUoW struct {
	w repository.BusinessWriteRepository
}

func (f fUoW) Businesses() repository.BusinessWriteRepository { return f.w }
func (f fUoW) SavePoint(context.Context, string) error        { return nil }
func (f fUoW) RollbackTo(context.Context, string) error       { return nil }

type fQW struct {
	r repository.BusinessReadRepository
}

func (f fQW) Businesses() repository.BusinessReadRepository { return f.r }

// fake managers
type fakeTx struct{ u fUoW }

func (m fakeTx) Do(_ context.Context, fn func(uow uow.UnitOfWork) error) error { return fn(m.u) }

type fakeQ struct{ q fQW }

func (m fakeQ) Do(_ context.Context, fn func(q uow.QueryWork) error) error { return fn(m.q) }

func TestBusinessService_Create(t *testing.T) {
	cases := []struct {
		name    string
		repoErr error
		wantID  int64
		wantErr bool
	}{
		{"ok", nil, 123, false},
		{"repo-error", errors.New("boom"), 0, true},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			wrepo := &fakeWriteRepo{err: tc.repoErr}
			svc := impl.NewBusinessService(fakeTx{u: fUoW{w: wrepo}}, fakeQ{q: fQW{r: &fakeReadRepo{}}})
			id, err := svc.Create(context.Background(), impl.CreateBusinessInput{Name: "acme", GovernmentID: "RUC1"})
			if tc.wantErr {
				require.Error(t, err)
				require.Equal(t, int64(0), id)
			} else {
				require.NoError(t, err)
				require.Equal(t, int64(123), id)
			}
		})
	}
}
